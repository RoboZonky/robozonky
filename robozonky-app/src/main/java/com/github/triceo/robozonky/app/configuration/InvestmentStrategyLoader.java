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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.triceo.robozonky.ExtensionsManager;
import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategyParseException;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements Java's {@link ServiceLoader} to provide suitable {@link InvestmentStrategy} implementations.
 */
class InvestmentStrategyLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvestmentStrategyLoader.class);
    private static final ServiceLoader<InvestmentStrategyService> STRATEGY_LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(InvestmentStrategyService.class);

    private static Optional<URL> convertToUrl(final String maybeUrl) {
        try {
            return Optional.of(new URL(maybeUrl));
        } catch (final MalformedURLException e) {
            try { // attempt to convert input to a file reference
                return Optional.of(new URL("file://" + maybeUrl));
            } catch (final MalformedURLException e1) {
                return Optional.empty();
            }
        }
    }

    private static Optional<InvestmentStrategy> loadWithFilename(final URL url, final String filename) {
        // FIXME the resource identified by the URL may specify a different encoding
        try (final BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream(), Defaults.CHARSET))) {
            final File tmp = File.createTempFile("robozonky-", "-" + filename);
            InvestmentStrategyLoader.LOGGER.info("Downloading strategy URL {} to {}.", url, tmp.toPath());
            Files.write(tmp.toPath(), r.lines().collect(Collectors.toList()));
            return InvestmentStrategyLoader.load(tmp);
        } catch (final Exception ex) {
            InvestmentStrategyLoader.LOGGER.error("Failed parsing strategy.", ex);
            return Optional.empty();
        }
    }

    private static Optional<InvestmentStrategy> loadWithFilename(final URL url) {
        return InvestmentStrategyLoader.loadWithFilename(url, "index.html");
    }

    static Optional<InvestmentStrategy> load(final String maybeUrl) {
        final Optional<URL> url = InvestmentStrategyLoader.convertToUrl(maybeUrl);
        if (!url.isPresent()) {
            InvestmentStrategyLoader.LOGGER.error("Unknown strategy location: {}.", maybeUrl);
        }
        final URL actualUrl = url.get();
        if (Objects.equals(actualUrl.getProtocol(), "file")) { // load the file directly
            return InvestmentStrategyLoader.load(new File(actualUrl.getPath()));
        } else { // download the resource identified by URL and store it to a file
            final String path = actualUrl.getPath();
            final String[] parts = path.split("\\Q/\\E");
            if (parts.length == 0) {
                return InvestmentStrategyLoader.loadWithFilename(actualUrl);
            }
            final String lastPart = parts[parts.length - 1].trim();
            if (lastPart.isEmpty()) {
                return InvestmentStrategyLoader.loadWithFilename(actualUrl);
            } else {
                return InvestmentStrategyLoader.loadWithFilename(actualUrl, lastPart);
            }
        }
    }

    static Optional<InvestmentStrategy> load(final File file) {
        if (file == null || !file.canRead()) {
            InvestmentStrategyLoader.LOGGER.error("Strategy file not accessible: {}.", file);
            return Optional.empty();
        }
        InvestmentStrategyLoader.LOGGER.trace("Loading strategies.");
        return StreamSupport.stream(InvestmentStrategyLoader.STRATEGY_LOADER.spliterator(), true)
                .peek(iss -> InvestmentStrategyLoader.LOGGER.debug("Evaluating strategy '{}' with '{}'.",
                        file.getAbsolutePath(), iss.getClass()))
                .map(iss -> {
                    try {
                        return iss.parse(file);
                    } catch (final InvestmentStrategyParseException ex) {
                        InvestmentStrategyLoader.LOGGER.error("Failed parsing strategy.", ex);
                        return Optional.empty();
                    }
                }).flatMap(o -> o.isPresent() ? Stream.of((InvestmentStrategy)o.get()) : Stream.empty())
                .findFirst();
    }


}

