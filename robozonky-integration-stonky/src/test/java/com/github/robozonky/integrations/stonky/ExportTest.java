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

package com.github.robozonky.integrations.stonky;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.UUID;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.google.api.client.http.FileContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class ExportTest {

    private static final String PEOPLE_CONTENT = "Some content in place of People XLS export.";
    private static final String WALLET_CONTENT = "Some content in place of Wallet XLS export.";

    private ClientAndServer server;
    private String serverUrl;
    private final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                                          300);

    @BeforeEach
    void startServer() {
        server = ClientAndServer.startClientAndServer(PortFactory.findFreePort());
        serverUrl = "127.0.0.1:" + server.getLocalPort();
        server.when(request()
                            .withPath("/users/me/investments/export/data")
                            .withQueryStringParameter("access_token", String.valueOf(token.getAccessToken())))
                .respond(response()
                                 .withHeader("Content-Disposition", "attachment; filename=\"people.xls\"")
                                 .withHeader("Content-Type", "application/force-download")
                                 .withHeader("Content-Transfer-Encoding", "binary")
                                 .withHeader("Content-Length", String.valueOf(PEOPLE_CONTENT.length()))
                                 .withBody(PEOPLE_CONTENT));
        server.when(request()
                            .withPath("/users/me/wallet/transactions/export/data")
                            .withQueryStringParameter("access_token", String.valueOf(token.getAccessToken())))
                .respond(response()
                                 .withHeader("Content-Disposition", "attachment; filename=\"wallet.xls\"")
                                 .withHeader("Content-Type", "application/force-download")
                                 .withHeader("Content-Transfer-Encoding", "binary")
                                 .withHeader("Content-Length", String.valueOf(WALLET_CONTENT.length()))
                                 .withBody(WALLET_CONTENT));
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    @Test
    void downloadPeople() throws IOException {
        final Export export = Export.PEOPLE;
        final FileContent f = export.download(token, "http://" + serverUrl);
        final OutputStream resulting = new ByteArrayOutputStream();
        f.writeTo(resulting);
        final String result = resulting.toString();
        assertSoftly(softly -> {
            softly.assertThat(f.getType()).isEqualTo("application/vnd.ms-excel");
            softly.assertThat(result).isEqualTo(PEOPLE_CONTENT);
        });
    }

    @Test
    void downloadWallet() throws IOException {
        final Export export = Export.WALLET;
        final FileContent f = export.download(token, "http://" + serverUrl);
        final OutputStream resulting = new ByteArrayOutputStream();
        f.writeTo(resulting);
        final String result = resulting.toString();
        assertSoftly(softly -> {
            softly.assertThat(f.getType()).isEqualTo("application/vnd.ms-excel");
            softly.assertThat(result).isEqualTo(WALLET_CONTENT);
        });
    }

    @Test
    void downloadFromWrongUrl() {
        final Export export = Export.WALLET;
        assertThatThrownBy(() -> export.download(token, serverUrl)) // missing protocol part of the URL
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(MalformedURLException.class);
    }

    @Test
    void downloadFromNonexistentRoot() {
        final Export export = Export.WALLET;
        assertThatThrownBy(() -> export.download(token, "http://localhost")) // not bound to anything
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(ConnectException.class);
    }

    @Test
    void downloadFromNonexistentPath() {
        final Export export = Export.WALLET;
        assertThatThrownBy(() -> export.download(token, "http://" + serverUrl + "/something")) // not bound to anything
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(FileNotFoundException.class);
    }
}
