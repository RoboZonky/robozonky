/*
 * Copyright 2020 The RoboZonky Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robozonky.app.version;

import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.util.StringUtil;
import com.github.robozonky.internal.util.UrlUtil;
import com.github.robozonky.internal.util.functional.Either;

/**
 * Retrieve latest released version from Maven Central. By default will check the REST API:
 * https://search.maven.org/classic/#api.
 * <p>
 * This class is based on the assumption that the search always returns versions in the decreasing order of
 * recency.
 */
final class MavenMetadataParser implements Function<String, Either<Throwable, Response>> {

    private static final Pattern PATTERN_STABLE_VERSION = Pattern.compile("\\A[1-9][0-9]*\\.[0-9]+\\.[0-9]+\\z");

    private final String groupId;
    private final String artifactId;
    private final String mavenCentralHostname;

    MavenMetadataParser(final String server) {
        this(server, "com.github.robozonky", "robozonky"); // RoboZonky's parent POM
    }

    MavenMetadataParser(final String server, final String groupId, final String artifactId) {
        this.mavenCentralHostname = server;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public MavenMetadataParser() {
        this("http://search.maven.org/");
    }

    /**
     * Assemble and read the Maven Central query URL from the given groupId and artifactId.
     * 
     * @param groupId    Group ID in question.
     * @param artifactId Artifact ID in question.
     * @param hostname   Maven Central search API hostname, such as "http://search.maven.org/"
     * @return Stream to read the Maven Central metadata from.
     * @throws IOException Network communications failure.
     */
    private static InputStream getMavenCentralData(final String groupId, final String artifactId, final String hostname)
            throws IOException {
        var url = hostname + "/solrsearch/select?q=g:%22" + groupId + "%22+AND+a:%22" + artifactId
                + "%22&core=gav&rows=100&wt=json";
        return UrlUtil.open(new URL(url))
            .getInputStream();
    }

    private static boolean isStable(final String version) {
        return PATTERN_STABLE_VERSION.matcher(version)
            .find();
    }

    static List<String> extractItems(final NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength())
            .mapToObj(nodeList::item)
            .map(Node::getTextContent)
            .collect(toList());
    }

    private static List<String> retrieveVersionStrings(final InputStream json) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.fromJson(json, CentralResponse.class)
                .getResponse()
                .getDocs()
                .stream()
                .map(CentralResponseGav::getV)
                .collect(Collectors.toUnmodifiableList());
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static List<String> centralSearchJsonToVersionStrings(final String source) {
        var inputStream = new ByteArrayInputStream(source.getBytes(Defaults.CHARSET));
        return retrieveVersionStrings(inputStream);
    }

    private static List<String> subListBefore(final List<String> items, final String item) {
        var indexOfItem = items.lastIndexOf(item);
        return items.subList(0, indexOfItem);
    }

    private static Either<Throwable, Response> processVersion(final String version, final List<String> knownVersions) {
        if (!knownVersions.contains(version)) {
            return Either.left(new IllegalStateException("Unknown RoboZonky version " + version));
        }
        var newerVersions = subListBefore(knownVersions, version);
        if (newerVersions.isEmpty()) {
            return Either.right(Response.noMoreRecentVersion());
        }
        return newerVersions.stream()
            .filter(MavenMetadataParser::isStable)
            .reduce((first, second) -> first) // first element in the stream of versions
            .map(stable -> {
                var evenNewerExperimentalVersions = subListBefore(newerVersions, stable);
                if (evenNewerExperimentalVersions.isEmpty()) {
                    return Either.<Throwable, Response>right(Response.moreRecentStable(stable));
                } else {
                    var experimental = evenNewerExperimentalVersions.get(0);
                    return Either.<Throwable, Response>right(Response.moreRecent(stable, experimental));
                }
            })
            .orElseGet(() -> Either.right(Response.moreRecentExperimental(newerVersions.get(0))));
    }

    private Either<Throwable, List<String>> getAvailableVersions() {
        try (var inputStream = getMavenCentralData(this.groupId, this.artifactId, this.mavenCentralHostname)) {
            var mavenMetadata = StringUtil.toString(inputStream);
            return Either.right(centralSearchJsonToVersionStrings(mavenMetadata));
        } catch (Exception ex) {
            return Either.left(ex);
        }
    }

    @Override
    public Either<Throwable, Response> apply(final String newVersion) {
        if (newVersion == null || newVersion.isEmpty() || newVersion.contains("SNAPSHOT")) {
            return Either.right(Response.noMoreRecentVersion());
        }
        return getAvailableVersions()
            .fold(Either::left, r -> processVersion(newVersion, r));
    }
}
