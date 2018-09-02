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

import java.util.Optional;
import java.util.function.Consumer;
import javax.ws.rs.core.Response;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

class StonkyTest extends AbstractRoboZonkyTest {

    private static final SecretProvider SECRET_PROVIDER = SecretProvider.inMemory("someone@somewhere.cz");
    private static final SessionInfo SESSION_INFO = new SessionInfo(SECRET_PROVIDER.getUsername());

    private final MultiRequestMockHttpTransport transport = new MultiRequestMockHttpTransport();
    private final OAuth oauth = mock(OAuth.class);
    private final Zonky zonky = mock(Zonky.class);
    private final ApiProvider api = mockApiProvider(oauth, zonky);

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

    @Test
    void noCredential() {
        final CredentialProvider credential = CredentialProvider.mock(false);
        final Stonky stonky = new Stonky(transport, credential, api);
        final Optional<String> result = stonky.apply(SECRET_PROVIDER);
        assertThat(result).isEmpty();
        verify(api).close();
    }

    @BeforeEach
    void emptyExports() {
        server.when(new HttpRequest()).respond(new HttpResponse()); // just return something to be downloaded
        final Response response = mock(Response.class);
        when(oauth.login(any(), any(), any())).thenAnswer(i -> mock(ZonkyApiToken.class));
        when(response.getStatus()).thenReturn(302);
        when(response.getHeaderString(any())).thenReturn("http://" + serverUrl + "/file");
        doReturn(response).when(zonky).downloadWalletExport();
        doReturn(response).when(zonky).downloadInvestmentsExport();
    }

    @Test
    void uglyCredential() {
        final CredentialProvider credential = new CredentialProvider() {
            @Override
            public boolean credentialExists(final SessionInfo sessionInfo) {
                return true;
            }

            @Override
            public Credential getCredential(final SessionInfo sessionInfo) {
                return null; // this is not allowed when the above is true
            }
        };
        final Stonky stonky = new Stonky(transport, credential, api);
        final Optional<String> result = stonky.apply(SECRET_PROVIDER);
        assertThat(result).isEmpty();
        verify(api).close();
    }

    private File getFolder(final String name) {
        final File result = GoogleUtil.getFile(name);
        result.setMimeType(DriveOverview.MIME_TYPE_FOLDER);
        return result;
    }

    @Nested
    class WithCredential {

        private final CredentialProvider credential = CredentialProvider.mock(true);
        private final File stonkyFolder = getFolder(DriveOverview.getFolderName(SESSION_INFO));
        private FilesInFolderResponseHandler stonkyFolderContent;
        private File copyOfStonkyMaster;

        @BeforeEach
        void filledPortfolio() {
            transport.addReponseHandler(new FilesInFolderResponseHandler("root", stonkyFolder));
            stonkyFolderContent = new FilesInFolderResponseHandler(stonkyFolder.getId());
            transport.addReponseHandler(stonkyFolderContent);
            final String stonkySpreadsheetToCopy = Properties.STONKY_MASTER.getValue()
                    .orElseThrow(IllegalStateException::new);
            // enable copying the master spreadsheet and the subsequent lookup of the copy by its ID
            final File stonkyMaster = GoogleUtil.getSpreadsheetFile("Some Stonky file",
                                                                    stonkySpreadsheetToCopy);
            final CopyFileResponseHandler h = new CopyFileResponseHandler(stonkyMaster);
            transport.addReponseHandler(h);
            transport.addReponseHandler(new GetFileResponseHandler(h.getSource()));
            copyOfStonkyMaster = h.getTarget();
            stonkyFolderContent.add(copyOfStonkyMaster);
            transport.addReponseHandler(new GetFileResponseHandler(copyOfStonkyMaster));
            transport.addReponseHandler(
                    new GetSpreadsheetResponseHandler(GoogleUtil.toSpreadsheet(copyOfStonkyMaster)));
            // enable the creation of new spreadsheets and copying sheets among them
            final Consumer<File> fileCreationNotififer = (f) -> {
                stonkyFolderContent.add(f);
                transport.addReponseHandler(new GetFileResponseHandler(f));
                final Spreadsheet parent = GoogleUtil.toSpreadsheet(f);
                transport.addReponseHandler(new GetSpreadsheetResponseHandler(parent));
                parent.getSheets()
                        .forEach(sheet -> transport.addReponseHandler(new CopySheetResponseHandler(parent, sheet)));
            };
            transport.addReponseHandler(new CreateFileResponseHandler(fileCreationNotififer));
            transport.addReponseHandler(new SpreadsheetBatchUpdateResponseHandler(copyOfStonkyMaster));
            transport.addReponseHandler(new DeleteFileResponseHandler());
        }

        @Test
        void passes() {
            final Stonky stonky = new Stonky(transport, credential, api);
            final Optional<String> result = stonky.apply(SECRET_PROVIDER);
            assertThat(result).contains(copyOfStonkyMaster.getId());
            verify(zonky).downloadInvestmentsExport();
            verify(zonky).downloadWalletExport();
        }
    }
}
