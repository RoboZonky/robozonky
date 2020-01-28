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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.functional.Either;
import com.github.robozonky.internal.util.StringUtil;
import com.github.robozonky.internal.util.UrlUtil;
import com.github.robozonky.internal.util.XmlUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Retrieve latest released version from Maven Central. By default will check
 * https://repo1.maven.org/maven2/com/github/robozonky/robozonky/maven-metadata.xml.
 * <p>
 * This class is based on the assumption that the Maven Metadata always report versions in the increasing order of
 * recency.
 */
final class MavenMetadataParser implements Function<String, Either<Throwable, Response>> {

    private static final String URL_SEPARATOR = "/";
    private static final Pattern PATTERN_DOT = Pattern.compile("\\Q.\\E");
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
        this("https://repo1.maven.org");
    }

    /**
     * Assemble the Maven Central metadata URL from the given groupId and artifactId.
     * @param groupId Group ID in question.
     * @param artifactId Artifact ID in question.
     * @param hostname Maven Central hostname, such as "https://repo1.maven.org"
     * @return Stream to read the Maven Central metadata from.
     * @throws IOException Network communications failure.
     */
    private static InputStream getMavenCentralData(final String groupId, final String artifactId,
                                                   final String hostname) throws IOException {
        var rootUrlParts = Stream.of(hostname, "maven2");
        var mavenGroupParts = Arrays.stream(MavenMetadataParser.PATTERN_DOT.split(groupId));
        var artifactParts = Stream.of(artifactId, "maven-metadata.xml");
        var url = Stream.concat(Stream.concat(rootUrlParts, mavenGroupParts), artifactParts)
                .collect(joining(URL_SEPARATOR));
        return UrlUtil.open(new URL(url));
    }

    private static boolean isStable(final String version) {
        return MavenMetadataParser.PATTERN_STABLE_VERSION.matcher(version).find();
    }

    static List<String> extractItems(final NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength())
                .mapToObj(nodeList::item)
                .map(Node::getTextContent)
                .collect(toList());
    }

    private static List<String> retrieveVersionStrings(final InputStream xml) throws ParserConfigurationException,
            IOException, SAXException, XPathExpressionException {
        var docFactory = XmlUtil.getDocumentBuilderFactory();
        var docBuilder = docFactory.newDocumentBuilder();
        var doc = docBuilder.parse(xml);
        var xPathFactory = XPathFactory.newInstance();
        var xpath = xPathFactory.newXPath();
        var expr = xpath.compile("/metadata/versioning/versions/version");
        return MavenMetadataParser.extractItems((NodeList) expr.evaluate(doc, XPathConstants.NODESET));
    }

    private static Either<Throwable, List<String>> mavenMetadataXmlToVersionStrings(final String source) {
        try (var inputStream = new ByteArrayInputStream(source.getBytes(Defaults.CHARSET))) {
            return Either.right(MavenMetadataParser.retrieveVersionStrings(inputStream));
        } catch (Exception ex) {
            return Either.left(ex);
        }
    }

    private static List<String> subListAfter(final List<String> items, final String item) {
        var indexOfItem = items.lastIndexOf(item);
        return items.subList(indexOfItem + 1, items.size());
    }

    private static String last(final List<String> items) {
        return items.get(items.size() - 1);
    }

    private static Either<Throwable, Response> processVersion(final String version, final List<String> knownVersions) {
        if (!knownVersions.contains(version)) {
            return Either.left(new IllegalStateException("Unknown RoboZonky version " + version));
        }
        var newerVersions = subListAfter(knownVersions, version);
        if (newerVersions.isEmpty()) {
            return Either.right(Response.noMoreRecentVersion());
        }
        return newerVersions.stream()
                .filter(MavenMetadataParser::isStable)
                .reduce((first, second) -> second) // last element in the stream of versions
                .map(stable -> {
                    var evenNewerExperimentalVersions = subListAfter(newerVersions, stable);
                    if (evenNewerExperimentalVersions.isEmpty()) {
                        return Either.<Throwable, Response>right(Response.moreRecentStable(stable));
                    } else {
                        var experimental = last(evenNewerExperimentalVersions);
                        return Either.<Throwable, Response>right(Response.moreRecent(stable, experimental));
                    }
                })
                .orElseGet(() -> Either.right(Response.moreRecentExperimental(last(newerVersions))));
    }

    private Either<Throwable, String> getLatestSource() {
        try (var inputStream = MavenMetadataParser.getMavenCentralData(this.groupId, this.artifactId,
                                                                       this.mavenCentralHostname)) {
            return Either.right(StringUtil.toString(inputStream));
        } catch (Exception ex) {
            return Either.left(ex);
        }
    }

    private Either<Throwable, List<String>> getAvailableVersions() {
        return getLatestSource()
                .fold(Either::left, MavenMetadataParser::mavenMetadataXmlToVersionStrings);
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
