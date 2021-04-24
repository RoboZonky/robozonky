/*
 * Copyright 2021 The RoboZonky Project
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
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.util.StringUtil;
import com.github.robozonky.internal.util.UrlUtil;
import com.github.robozonky.internal.util.functional.Either;

/**
 * Retrieve latest released version from Github API.
 */
final class GithubMetadataParser implements Function<String, Either<Throwable, Response>> {

    private final String server;

    GithubMetadataParser(final String server) {
        this.server = Objects.requireNonNull(server);
    }

    GithubMetadataParser() {
        this("https://api.github.com");
    }

    private static List<GithubRelease> retrieveReleases(final InputStream json) {
        ParameterizedType releaseListType = TypeUtils.parameterize(List.class, GithubRelease.class);
        try (var jsonb = JsonbBuilder.create()) {
            return ((List<GithubRelease>) jsonb.fromJson(json, releaseListType))
                .stream()
                .collect(Collectors.toUnmodifiableList());
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static List<GithubRelease> jsonToVersionStrings(final String source) {
        var inputStream = new ByteArrayInputStream(source.getBytes(Defaults.CHARSET));
        return retrieveReleases(inputStream);
    }

    private static Response processVersion(final String currentVersion, final List<GithubRelease> knownVersions) {
        // Assumes release name in the Maven version format.
        // ("RoboZonky X.Y.Z", possibly with a suffix of "-beta-W", "-cr-W" etc.)
        var knownVersionName = "RoboZonky " + currentVersion;
        // Assumes that versions are sorted in the decreasing order of recency.
        var newerReleases = knownVersions.stream()
            .takeWhile(release -> !Objects.equals(release.getName(), knownVersionName))
            .collect(Collectors.toUnmodifiableList());
        var latestRelease = newerReleases.stream()
            .filter(f -> !f.isDraft())
            .filter(f -> !f.isPrerelease())
            .findFirst();
        var latestExperimentalRelease = newerReleases.stream()
            .filter(f -> !f.isDraft())
            .filter(GithubRelease::isPrerelease)
            .findFirst();

        // At this point, we know that latestRelease is more recent than current, if exists.
        // latestExperimentalRelease is more recent than current, if exists.
        return latestExperimentalRelease.map(experimentalRelease -> latestRelease.map(release -> {
            if (experimentalRelease.getDatePublished()
                .isAfter(release.getDatePublished())) {
                return Response.moreRecent(release, experimentalRelease);
            } else {
                return Response.moreRecentStable(release);
            }
        })
            .orElseGet(() -> Response.moreRecentExperimental(experimentalRelease)))
            .orElseGet(() -> latestRelease.map(Response::moreRecentStable)
                .orElseGet(Response::noMoreRecentVersion));
    }

    private static InputStream getGithubData(String server) throws IOException {
        var url = server + "/repos/robozonky/robozonky/releases";
        return UrlUtil.open(new URL(url))
            .getInputStream();
    }

    private Either<Throwable, List<GithubRelease>> getAvailableVersions() {
        try (var inputStream = getGithubData(server)) {
            return Either.right(jsonToVersionStrings(StringUtil.toString(inputStream)));
        } catch (Exception ex) {
            return Either.left(ex);
        }
    }

    @Override
    public Either<Throwable, Response> apply(final String currentVersion) {
        if (currentVersion == null || Objects.equals(currentVersion, "unknown") || currentVersion.isEmpty() ||
                currentVersion.contains("SNAPSHOT")) {
            return Either.right(Response.noMoreRecentVersion());
        }
        return getAvailableVersions()
            .fold(Either::left, r -> Either.right(processVersion(currentVersion, r)));
    }
}
