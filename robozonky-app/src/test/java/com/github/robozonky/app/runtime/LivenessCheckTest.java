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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class LivenessCheckTest {

    private ClientAndServer server;
    private String serverUrl;

    @BeforeEach
    void startServer() {
        server = ClientAndServer.startClientAndServer(PortFactory.findFreePort());
        serverUrl = "127.0.0.1:" + server.getLocalPort();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    @AfterEach
    void resumeSchedulers() {
        Schedulers.INSTANCE.resume(); // reset
    }

    @Test
    void check() {
        server
                .when(request())
                .respond(response()
                                 .withStatusCode(200)
                                 .withBody(ApiVersionTest.SAMPLE));
        final LivenessCheck l = new LivenessCheck("http://" + serverUrl);
        l.run();
        assertThat(l.get()).isPresent();
        assertThat(l.get().get()).isEqualTo("0.77.0");
        // now make the server fail and re-check that the instance was updated
        server
                .reset()
                .when(request())
                .respond(response()
                                 .withStatusCode(500));
        l.run();
        assertThat(l.get()).isEmpty();
    }

    @Test
    void wrongUrl() {
        server
                .when(request())
                .respond(response()
                                 .withStatusCode(500));
        final LivenessCheck l = new LivenessCheck(serverUrl); // no protocol
        l.run();
        assertThat(l.get()).isEmpty();
    }

    @Test
    void malformedResponse() {
        server
                .when(request())
                .respond(response()
                                 .withStatusCode(200)
                                 .withBody(UUID.randomUUID().toString()));
        final LivenessCheck l = new LivenessCheck("http://" + serverUrl);
        l.run();
        assertThat(l.get()).isEmpty();
    }
}
