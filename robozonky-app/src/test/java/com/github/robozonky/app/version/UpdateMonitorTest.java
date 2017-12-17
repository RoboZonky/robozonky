/*
 * Copyright 2017 The RoboZonky Project
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

import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.socket.PortFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UpdateMonitorTest {

    private ClientAndServer server;
    private String serverUrl;

    @Before
    public void startServer() {
        server = ClientAndServer.startClientAndServer(PortFactory.findFreePort());
        serverUrl = "http://127.0.0.1:" + server.getPort();
    }

    @After
    public void stopServer() {
        server.stop();
    }

    @Test
    public void checkRetrieval() throws Exception {
        server.when(HttpRequest.request().withPath("/maven2/com/github/robozonky/robozonky/maven-metadata.xml"))
                .respond(HttpResponse.response().withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                  "<metadata>\n" +
                                                                  "  <groupId>com.github.robozonky</groupId>\n" +
                                                                  "  <artifactId>robozonky</artifactId>\n" +
                                                                  "  <versioning>\n" +
                                                                  "    <latest>4.0.1</latest>\n" +
                                                                  "    <release>4.0.1</release>\n" +
                                                                  "    <versions>\n" +
                                                                  "      <version>4.0.0-cr-1</version>\n" +
                                                                  "      <version>4.0.0</version>\n" +
                                                                  "      <version>4.0.1</version>\n" +
                                                                  "    </versions>\n" +
                                                                  "    <lastUpdated>20171015201449</lastUpdated>\n" +
                                                                  "  </versioning>\n" +
                                                                  "</metadata>"));
        final UpdateMonitor v = new UpdateMonitor(serverUrl);
        v.run();
        Assertions.assertThat(v.get()).isPresent();
    }

    @Test
    public void checkNonExistentUrl() throws Exception {
        server.when(HttpRequest.request()).respond(HttpResponse.notFoundResponse());
        final UpdateMonitor v = new UpdateMonitor(serverUrl, "com.github.robozonky", "robozonky-nonexistent");
        v.run();
        Assertions.assertThat(v.get()).isEmpty();
    }

    @Test
    public void checkNoStable() {
        Assertions.assertThatThrownBy(() -> UpdateMonitor.findFirstStable(Collections.singleton("1.2.0-beta-1")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void parseSingleNodeList() {
        final String version = "1.2.3";
        final Node n = Mockito.mock(Node.class);
        Mockito.when(n.getTextContent()).thenReturn(version);
        final NodeList l = Mockito.mock(NodeList.class);
        Mockito.when(l.getLength()).thenReturn(1);
        Mockito.when(l.item(ArgumentMatchers.eq(0))).thenReturn(n);
        final VersionIdentifier actual = UpdateMonitor.parseNodeList(l);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getLatestStable()).isEqualTo(version);
            softly.assertThat(actual.getLatestUnstable()).isEmpty();
        });
    }

    @Test
    public void parseLongerNodeList() {
        final String version = "1.2.3", version2 = "1.2.4-SNAPSHOT";
        final Node n1 = Mockito.mock(Node.class);
        Mockito.when(n1.getTextContent()).thenReturn(version);
        final Node n2 = Mockito.mock(Node.class);
        Mockito.when(n2.getTextContent()).thenReturn(version2);
        final NodeList l = Mockito.mock(NodeList.class);
        Mockito.when(l.getLength()).thenReturn(2);
        Mockito.when(l.item(ArgumentMatchers.eq(0))).thenReturn(n1);
        Mockito.when(l.item(ArgumentMatchers.eq(1))).thenReturn(n2);
        final VersionIdentifier actual = UpdateMonitor.parseNodeList(l);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getLatestStable()).isEqualTo(version);
            softly.assertThat(actual.getLatestUnstable()).contains(version2);
        });
    }
}
