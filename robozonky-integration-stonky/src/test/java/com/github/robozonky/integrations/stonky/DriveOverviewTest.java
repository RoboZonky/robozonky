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

import java.io.IOException;

import com.github.robozonky.api.SessionInfo;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential;
import com.google.api.services.drive.Drive;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DriveOverviewTest {

    private static final SessionInfo SESSION_INFO = new SessionInfo("someone@somewhere.cz");

    @Test
    void emptyGoogleDrive() throws IOException {
        final Credential credential = new MockGoogleCredential.Builder().build();
        final MultiRequestMockHttpTransport transport = new MultiRequestMockHttpTransport();
        transport.addReponseHandler(new ListAllFilesResponseHandler());
        final Drive service = Util.createDriveService(credential, transport);
        final DriveOverview overview = DriveOverview.create(SESSION_INFO, service);
        assertThat(overview).isNotNull();
    }
}
