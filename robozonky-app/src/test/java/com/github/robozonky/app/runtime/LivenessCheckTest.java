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

import java.util.UUID;

import com.github.robozonky.util.Schedulers;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class LivenessCheckTest {

    private static final String SAMPLE = "{\"branch\":\"origin/master\"," +
            "\"commitId\":\"e51d4fcb9eac1a9599a64c93c181325a2c38e779\"," +
            "\"commitIdAbbrev\":\"e51d4fc\"," +
            "\"buildTime\":\"2018-01-18T20:16:08+0100\"," +
            "\"buildVersion\":\"0.77.0\"," +
            "\"tags\":[\"0.77.0\"]}";

    private ClientAndServer server;
    private String serverUrl;

    @BeforeEach
    public void startServer() {
        server = ClientAndServer.startClientAndServer(PortFactory.findFreePort());
        serverUrl = "127.0.0.1:" + server.getPort();
    }

    @AfterEach
    public void stopServer() {
        server.stop();
    }

    @AfterEach
    public void resumeSchedulers() {
        Schedulers.INSTANCE.resume(); // reset
    }

    @Test
    public void check() {
        server
                .when(request())
                .respond(response()
                                 .withStatusCode(200)
                                 .withBody(SAMPLE));
        final LivenessCheck l = new LivenessCheck("http://" + serverUrl);
        l.run();
        Assertions.assertThat(l.get()).isPresent();
        Assertions.assertThat(l.get().get().getBuildVersion()).isEqualTo("0.77.0");
        Schedulers.INSTANCE.resume(); // reset
    }

    @Test
    public void wrongResponse() {
        server
                .when(request())
                .respond(response()
                                 .withStatusCode(500));
        final LivenessCheck l = new LivenessCheck("http://" + serverUrl);
        l.run();
        Assertions.assertThat(l.get()).isEmpty();
    }

    @Test
    public void wrongUrl() {
        server
                .when(request())
                .respond(response()
                                 .withStatusCode(500));
        final LivenessCheck l = new LivenessCheck(serverUrl); // no protocol
        l.run();
        Assertions.assertThat(l.get()).isEmpty();
    }

    @Test
    public void malformedResponse() {
        server
                .when(request())
                .respond(response()
                                 .withStatusCode(200)
                                 .withBody(UUID.randomUUID().toString()));
        final LivenessCheck l = new LivenessCheck("http://" + serverUrl);
        l.run();
        Assertions.assertThat(l.get()).isEmpty();
        Schedulers.INSTANCE.resume(); // reset
    }
}
