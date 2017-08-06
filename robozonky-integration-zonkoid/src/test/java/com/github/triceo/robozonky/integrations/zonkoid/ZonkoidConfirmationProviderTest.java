/*
 * Copyright 2017 Lukáš Petrovický
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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import com.github.triceo.robozonky.api.confirmations.RequestId;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.once;

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
                              .withHeader("User-Agent", Defaults.ROBOZONKY_USER_AGENT),
                      once());
    }

    private boolean execute(final int code) {
        this.mockServerResponse(code);
        final boolean result =
                ZonkoidConfirmationProvider.requestConfirmation(new RequestId("user@somewhere.cz",
                                                                              "apitest".toCharArray()), 1, 200,
                                                                serverUrl);
        this.verifyClientRequest();
        return result;
    }

    @Test
    public void normalResponse() {
        final boolean result = this.execute(200);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void unknownResponse() {
        final boolean result = this.execute(500);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void failingReponse1() {
        final boolean result = this.execute(400);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void failingResponse2() {
        final boolean result = this.execute(403);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void md5() throws NoSuchAlgorithmException {
        final String in = "654321|ROBOZONKY|name@surname.cz|12345";
        final String out = "cd15efe487e98e83a215091221568eda";
        Assertions.assertThat(ZonkoidConfirmationProvider.md5(in)).isEqualTo(out);
    }

    @Test
    public void errorOverHttps() throws NoSuchAlgorithmException {
        final boolean result = ZonkoidConfirmationProvider.handleError(null, 0, 0, "some", "https",
                                                                       new RuntimeException());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void errorOverHttp() throws NoSuchAlgorithmException {
        final boolean result = ZonkoidConfirmationProvider.handleError(null, 0, 0, "some", "http",
                                                                       new RuntimeException());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void errorOverUnknown() {
        Assertions.assertThatThrownBy(() -> ZonkoidConfirmationProvider.handleError(null, 0, 0, "some",
                                                                                    UUID.randomUUID().toString(),
                                                                                    new RuntimeException()))
                .isInstanceOf(
                        IllegalStateException.class);
    }

    @Test
    public void properHttpPost() throws UnsupportedEncodingException {
        final int loanId = 1;
        final RequestId r = new RequestId("user@somewhere.cz", "apitest".toCharArray());
        final HttpPost post = ZonkoidConfirmationProvider.getRequest(r, loanId, 200, "https", "somewhere");
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(post.getFirstHeader("Content-Type").getValue()).isEqualTo(
                "application/x-www-form-urlencoded");
        softly.assertThat(post.getFirstHeader("Authorization").getValue())
                .isEqualTo(ZonkoidConfirmationProvider.getAuthenticationString(r, loanId));
        softly.assertThat(post.getEntity()).isInstanceOf(UrlEncodedFormEntity.class);
        softly.assertAll();
    }

    @Test
    public void properId() {
        final ZonkoidConfirmationProvider p = new ZonkoidConfirmationProvider();
        Assertions.assertThat(p.getId()).contains("Zonkoid").contains("Zonkios");
    }
}
