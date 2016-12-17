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

package com.github.triceo.robozonky.app.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategyParseException;
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

    private static final String URL = "/robozonky-strategy.cfg";
    private static ClientAndServer mockServer;

    @BeforeClass
    public static void setupServer() throws IOException {
        InvestmentStrategyLoaderTest.mockServer = ClientAndServer.startClientAndServer(PortFactory.findFreePort());
        final File strategyFile = new File(IoTestUtil.findMainSource("assembly", "resources"),
                "robozonky-conservative.cfg");
        final String fileContents = Files.readAllLines(strategyFile.toPath()).stream()
                .collect(Collectors.joining(System.lineSeparator()));
        InvestmentStrategyLoaderTest.mockServer
                .when(request().withPath(InvestmentStrategyLoaderTest.URL))
                .respond(response().withBody(fileContents));
    }

    @AfterClass
    public static void stopServer() {
        InvestmentStrategyLoaderTest.mockServer.reset();
        InvestmentStrategyLoaderTest.mockServer.stop();
    }

    @Test(expected = InvestmentStrategyParseException.class)
    public void nullStrategyAsFile() throws InvestmentStrategyParseException {
        InvestmentStrategyLoader.load((String)null);
    }

    @Test(expected = InvestmentStrategyParseException.class)
    public void nullStrategyAsString() throws InvestmentStrategyParseException {
        InvestmentStrategyLoader.load((File)null);
    }

    @Test(expected = InvestmentStrategyParseException.class)
    public void nonExistentStrategyAsFile() throws InvestmentStrategyParseException, IOException {
        final File strategy = File.createTempFile("robozonky-", ".cfg");
        Assume.assumeTrue(strategy.delete());
        InvestmentStrategyLoader.load(strategy);
    }

    @Test(expected = InvestmentStrategyParseException.class)
    public void nonExistentStrategyAsUrl() throws InvestmentStrategyParseException, IOException {
        final File strategy = File.createTempFile("robozonky-", ".cfg");
        Assume.assumeTrue(strategy.delete());
        InvestmentStrategyLoader.load("file://" + strategy.toPath());
    }

    @Test
    public void strategyAsUrl() throws InvestmentStrategyParseException {
        final String url = "http://127.0.0.1:" + InvestmentStrategyLoaderTest.mockServer.getPort() +
                InvestmentStrategyLoaderTest.URL;
        final Optional<InvestmentStrategy> result = InvestmentStrategyLoader.load(url);
        Assertions.assertThat(result).isPresent();
    }
}
