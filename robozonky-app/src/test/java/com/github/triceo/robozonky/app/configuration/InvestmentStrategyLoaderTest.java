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

package com.github.triceo.robozonky.app.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.util.IoTestUtil;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import static org.mockserver.model.HttpRequest.*;
import static org.mockserver.model.HttpResponse.*;

public class InvestmentStrategyLoaderTest {

    private static final String URL_STRING = "/robozonky-strategy.cfg";
    private static ClientAndServer mockServer;

    @BeforeClass
    public static void setupServer() throws IOException {
        InvestmentStrategyLoaderTest.mockServer = ClientAndServer.startClientAndServer(PortFactory.findFreePort());
        final File strategyFile = new File(IoTestUtil.findMainSource("assembly", "resources"),
                "robozonky-conservative.cfg");
        final String fileContents = Files.readAllLines(strategyFile.toPath()).stream()
                .collect(Collectors.joining(System.lineSeparator()));
        InvestmentStrategyLoaderTest.mockServer
                .when(request().withPath(InvestmentStrategyLoaderTest.URL_STRING))
                .respond(response().withBody(fileContents));
        InvestmentStrategyLoaderTest.mockServer
                .when(request().withPath("/"))
                .respond(response().withBody(fileContents));
    }

    @AfterClass
    public static void stopServer() {
        InvestmentStrategyLoaderTest.mockServer.reset();
        InvestmentStrategyLoaderTest.mockServer.stop();
    }

    @Test
    public void nonExistentStrategyAsUrl() throws IOException {
        final File strategy = File.createTempFile("robozonky-", ".cfg");
        Assume.assumeTrue(strategy.delete());
        Assertions.assertThat(InvestmentStrategyLoader.load("file://" + strategy.toPath())).isEmpty();
    }

    @Test
    public void strategyAsMaybeUrl() {
        final String url = "http://127.0.0.1:" + InvestmentStrategyLoaderTest.mockServer.getPort() +
                InvestmentStrategyLoaderTest.URL_STRING;
        final Optional<InvestmentStrategy> result = InvestmentStrategyLoader.load(url);
        Assertions.assertThat(result).isPresent();
    }

    @Test
    public void strategyAsMaybeUrlRoot() {
        final String url = "http://127.0.0.1:" + InvestmentStrategyLoaderTest.mockServer.getPort() + "/";
        final Optional<InvestmentStrategy> result = InvestmentStrategyLoader.load(url);
        Assertions.assertThat(result).isPresent();
    }

    @Test
    public void strategyAsWrongUrl() {
        final String url = "noprotocol://somewhere";
        final Optional<InvestmentStrategy> result = InvestmentStrategyLoader.load(url);
        Assertions.assertThat(result).isEmpty();
    }

}
