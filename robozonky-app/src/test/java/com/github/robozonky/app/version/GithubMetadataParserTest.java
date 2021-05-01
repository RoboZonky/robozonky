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

import static org.assertj.core.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.test.AbstractRoboZonkyTest;

@ExtendWith(MockServerExtension.class)
class GithubMetadataParserTest extends AbstractRoboZonkyTest {

    private final ClientAndServer server;
    private final String serverUrl;

    public GithubMetadataParserTest(ClientAndServer server) {
        this.server = server;
        this.serverUrl = "http://127.0.0.1:" + server.getLocalPort();
    }

    @Test
    void checkNullVersion() {
        var parser = new GithubMetadataParser(serverUrl);
        var result = parser.apply(null);
        assertThat(result.get()).isEqualTo(Response.noMoreRecentVersion());
    }

    @Test
    void checkEmptyVersion() {
        var parser = new GithubMetadataParser(serverUrl);
        var result = parser.apply("");
        assertThat(result.get()).isEqualTo(Response.noMoreRecentVersion());
    }

    @Test
    void checkUnknownVersion() {
        var parser = new GithubMetadataParser(serverUrl);
        var result = parser.apply("unknown"); // Field set to a default value.
        assertThat(result.get()).isEqualTo(Response.noMoreRecentVersion());
    }

    @Nested
    class ValidMetadata {

        @BeforeEach
        void setupMetadata() throws IOException { // This is a mocked response of the REST API.
            var body = IOUtils.toString(getClass().getResourceAsStream("github.json"), Defaults.CHARSET);
            server.when(HttpRequest.request()
                .withPath("/repos/robozonky/robozonky/releases"))
                .respond(HttpResponse.response()
                    .withBody(body));
        }

        @Test
        void checkLatestVersionOutdated() {
            var parser = new GithubMetadataParser(serverUrl);
            var result = parser.apply("6.3.0");
            assertThat(result.get())
                .extracting(Response::getMoreRecentStableVersion,
                        InstanceOfAssertFactories.optional(GithubRelease.class))
                .hasValueSatisfying(r -> assertThat(r.getName()).isEqualTo("RoboZonky 6.4.0"));
            assertThat(result.get())
                .extracting(Response::getMoreRecentExperimentalVersion,
                        InstanceOfAssertFactories.optional(GithubRelease.class))
                .isEmpty();
        }

        @Test
        void checkLatestVersionNoneMoreRecent() {
            var parser = new GithubMetadataParser(serverUrl);
            var result = parser.apply("6.4.0");
            assertThat(result.get()).isEqualTo(Response.noMoreRecentVersion());
        }
    }

    @Nested
    class FailingMetadata {

        @BeforeEach
        void setupMetadata() {
            server.when(HttpRequest.request()
                .withPath("/repos/robozonky/robozonky/releases"))
                .respond(HttpResponse.notFoundResponse());
        }

        @Test
        void checkUnknownVersion() {
            var parser = new GithubMetadataParser(serverUrl);
            var result = parser.apply(UUID.randomUUID()
                .toString());
            assertThat(result.getLeft())
                .isInstanceOf(FileNotFoundException.class);
        }

    }
}
