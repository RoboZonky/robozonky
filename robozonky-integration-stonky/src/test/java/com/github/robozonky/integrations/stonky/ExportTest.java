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

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.core.Response;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.socket.PortFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportTest extends AbstractRoboZonkyTest {

    private final Zonky zonky = mock(Zonky.class);
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

    @BeforeEach
    void emptyExports() {
        server.when(new HttpRequest()).respond(new HttpResponse().withBody("")); // just return something to be downloaded
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(302);
        when(response.getHeaderString(any())).thenReturn("http://" + serverUrl + "/file");
        doReturn(response).when(zonky).downloadWalletExport();
        doReturn(response).when(zonky).downloadInvestmentsExport();
    }

    @Test
    void triggersWalletDownload() throws ExecutionException, InterruptedException {
        final OAuth auth = mock(OAuth.class);
        final ApiProvider api = mockApiProvider(auth, zonky);
        final CompletableFuture<Optional<File>> c = Export.WALLET.download(api, () -> mock(ZonkyApiToken.class),
                                                                           () -> mock(ZonkyApiToken.class));
        c.get();
        assertThat(c.get()).isPresent();
        verify(zonky).downloadWalletExport();
    }

    @Test
    void triggersInvestmentsDownload() throws ExecutionException, InterruptedException {
        final OAuth auth = mock(OAuth.class);
        final ApiProvider api = mockApiProvider(auth, zonky);
        final CompletableFuture<Optional<File>> c = Export.INVESTMENTS.download(api, () -> mock(ZonkyApiToken.class),
                                                                                () -> mock(ZonkyApiToken.class));
        assertThat(c.get()).isPresent();
        verify(zonky).downloadInvestmentsExport();
    }
}
