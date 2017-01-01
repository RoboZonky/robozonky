/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.integrations.zonkoid;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.confirmations.Confirmation;
import com.github.triceo.robozonky.api.confirmations.ConfirmationType;
import com.github.triceo.robozonky.api.confirmations.RequestId;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import static org.mockserver.model.HttpRequest.*;
import static org.mockserver.model.HttpResponse.*;
import static org.mockserver.verify.VerificationTimes.*;

public class ZonkoidConfirmationProviderTest {

    private ClientAndServer server;
    private String serverUrl;

    @Before
    public void startServer() {
        server = ClientAndServer.startClientAndServer(PortFactory.findFreePort());
        serverUrl = "127.0.0.1:" + server.getPort();
    }

    @After
    public void stopServer() {
        server.stop();
    }

    private void mockServerResponse(final int code) {
        server.when(request().withPath(ZonkoidConfirmationProvider.PATH)).respond(response().withStatusCode(code));
    }

    private void verifyClientRequest() {
        server.verify(request()
                .withPath(ZonkoidConfirmationProvider.PATH)
                .withHeader("Accept", "text/plain")
                .withHeader("Authorization")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
                .withHeader("User-Agent", Defaults.ROBOZONKY_USER_AGENT),
                once());
    }

    private Optional<Confirmation> execute(final int code) {
        this.mockServerResponse(code);
        final ZonkoidConfirmationProvider provider = new ZonkoidConfirmationProvider();
        final Optional<Confirmation> result = provider.requestConfirmation(new RequestId("user@somewhere.cz",
                        "apitest".toCharArray()), 1, 200, serverUrl);
        this.verifyClientRequest();
        return result;
    }

    @Test
    public void normalResponse() {
        final Optional<Confirmation> result = this.execute(200);
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getType()).isEqualTo(ConfirmationType.DELEGATED);
    }

    @Test
    public void unknownResponse() {
        final Optional<Confirmation> result = this.execute(500);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void failingReponse1() {
        final Optional<Confirmation> result = this.execute(400);
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getType()).isEqualTo(ConfirmationType.REJECTED);
    }

    @Test
    public void failingResponse2() {
        final Optional<Confirmation> result = this.execute(403);
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getType()).isEqualTo(ConfirmationType.REJECTED);
    }

    @Test
    public void md5() throws NoSuchAlgorithmException {
        final String in = "654321|ROBOZONKY|name@surname.cz|12345";
        final String out = "cd15efe487e98e83a215091221568eda";
        Assertions.assertThat(ZonkoidConfirmationProvider.md5(in)).isEqualTo(out);
    }

    @Test
    public void errorOverHttp() throws NoSuchAlgorithmException {
        final Optional<Confirmation> result = ZonkoidConfirmationProvider.handleError(null, 0, 0, "some",
                "http", new RuntimeException());
        Assertions.assertThat(result).isEmpty();
    }

    @Test(expected = IllegalStateException.class)
    public void errorOverUnknown() {
        final Optional<Confirmation> result = ZonkoidConfirmationProvider.handleError(null, 0, 0, "some",
                UUID.randomUUID().toString(), new RuntimeException());
        Assertions.assertThat(result).isEmpty();
    }

}
