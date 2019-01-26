/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.integrations.zonkoid;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import com.github.robozonky.api.confirmations.RequestId;
import com.github.robozonky.internal.api.Defaults;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.once;

class ZonkoidConfirmationProviderTest {

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

    private void mockServerResponse(final int code) {
        server.when(request().withPath(ZonkoidConfirmationProvider.PATH)).respond(response().withStatusCode(code));
    }

    private void verifyClientRequest() {
        server.verify(request().withPath(ZonkoidConfirmationProvider.PATH), once());
    }

    private boolean execute(final int code) {
        this.mockServerResponse(code);
        final RequestId id = new RequestId("user@somewhere.cz", "apitest".toCharArray());
        final ZonkoidConfirmationProvider zcp = new ZonkoidConfirmationProvider(serverUrl);
        final boolean result = zcp.requestConfirmation(id , 1, 200);
        this.verifyClientRequest();
        return result;
    }

    @Test
    void normalResponse() {
        final boolean result = this.execute(200);
        assertThat(result).isTrue();
    }

    @Test
    void unknownResponse() {
        final boolean result = this.execute(500);
        assertThat(result).isFalse();
    }

    @Test
    void failingReponse1() {
        final boolean result = this.execute(400);
        assertThat(result).isFalse();
    }

    @Test
    void failingResponse2() {
        final boolean result = this.execute(403);
        assertThat(result).isFalse();
    }

    @Test
    void md5() throws NoSuchAlgorithmException {
        final String in = "654321|ROBOZONKY|name@surname.cz|12345";
        final String out = "cd15efe487e98e83a215091221568eda";
        assertThat(ZonkoidConfirmationProvider.md5(in)).isEqualTo(out);
    }

    @Test
    void errorOverHttps() {
        final boolean result = ZonkoidConfirmationProvider.handleError(null, 0, 0, "some", "https",
                                                                       new RuntimeException());
        assertThat(result).isFalse();
    }

    @Test
    void errorOverHttp() {
        final boolean result = ZonkoidConfirmationProvider.handleError(null, 0, 0, "some", "http",
                                                                       new RuntimeException());
        assertThat(result).isFalse();
    }

    @Test
    void errorOverUnknown() {
        assertThatThrownBy(() -> ZonkoidConfirmationProvider.handleError(null, 0, 0, "some",
                                                                         UUID.randomUUID().toString(),
                                                                         new RuntimeException()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void properHttpPost() throws UnsupportedEncodingException {
        final int loanId = 1;
        final RequestId r = new RequestId("user@somewhere.cz", "apitest".toCharArray());
        final HttpPost post = ZonkoidConfirmationProvider.getRequest(r, loanId, 200, "https", "somewhere");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(post.getFirstHeader("Accept").getValue()).isEqualTo("text/plain");
            softly.assertThat(post.getFirstHeader("Authorization").getValue())
                    .isNotEmpty()
                    .isEqualTo(ZonkoidConfirmationProvider.getAuthenticationString(r, loanId));
            softly.assertThat(post.getFirstHeader("Content-Type").getValue()).isEqualTo(
                    "application/x-www-form-urlencoded");
            softly.assertThat(post.getEntity()).isInstanceOf(UrlEncodedFormEntity.class);
            softly.assertThat(post.getFirstHeader("User-Agent").getValue()).isEqualTo(Defaults.ROBOZONKY_USER_AGENT);
        });
    }

    @Test
    void properId() {
        final ZonkoidConfirmationProvider p = new ZonkoidConfirmationProvider();
        assertThat(p.getId()).contains("Zonkoid").contains("Zonkios");
    }
}
