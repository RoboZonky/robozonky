/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.runtime;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.robozonky.internal.api.Defaults;

/**
 * Represents version of the Zonky API as returned by a remote resource. This doesn't use RESTEasy as it's designed to
 * be run very frequently with minimal resource requirements.
 */
class ApiVersion {

    private static final ObjectMapper MAPPER = new ObjectMapper(); // stored for performance reasons
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ",
                                                                                   Defaults.LOCALE);
    private final String branch, commitId, commitIdAbbrev, buildVersion;
    private final OffsetDateTime buildTime;
    private final OffsetDateTime currentApiTime;
    private final Collection<String> tags;

    ApiVersion(final String branch, final String commitId, final String commitIdAbbrev, final OffsetDateTime buildTime,
               final String buildVersion, final OffsetDateTime apiTime, final String... tags) {
        this.branch = branch;
        this.commitId = commitId;
        this.commitIdAbbrev = commitIdAbbrev;
        this.buildTime = buildTime;
        this.currentApiTime = apiTime;
        this.buildVersion = buildVersion;
        this.tags = Stream.of(tags).collect(Collectors.toSet());
    }

    public static ApiVersion read(final String json) throws IOException {
        final JsonNode actualObj = MAPPER.readTree(json);
        final String branch = actualObj.get("branch").asText();
        final String commitId = actualObj.get("commitId").asText();
        final String commitIdAbbrev = actualObj.get("commitIdAbbrev").asText();
        final String buildVersion = actualObj.get("buildVersion").asText();
        final OffsetDateTime buildTime = OffsetDateTime.parse(actualObj.get("buildTime").asText(), FORMATTER);
        final OffsetDateTime apiTime = OffsetDateTime.parse(actualObj.get("currentApiTime").asText()); // ISO8601 format
        final Iterable<JsonNode> n = actualObj.withArray("tags");
        final String[] tags = StreamSupport.stream(n.spliterator(), false)
                .map(JsonNode::asText)
                .toArray(String[]::new);
        return new ApiVersion(branch, commitId, commitIdAbbrev, buildTime, buildVersion, apiTime, tags);
    }

    public String getBranch() {
        return branch;
    }

    public OffsetDateTime getBuildTime() {
        return buildTime;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getCommitIdAbbrev() {
        return commitIdAbbrev;
    }

    public String getBuildVersion() {
        return buildVersion;
    }

    public OffsetDateTime getCurrentApiTime() {
        return currentApiTime;
    }

    public Collection<String> getTags() {
        return Collections.unmodifiableCollection(tags);
    }
}
