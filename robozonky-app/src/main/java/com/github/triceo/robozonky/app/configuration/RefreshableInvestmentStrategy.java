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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.util.Scheduler;

class RefreshableInvestmentStrategy extends Refreshable<InvestmentStrategy> {

    private static URL convertToUrl(final String maybeUrl) {
        try {
            return new URL(maybeUrl);
        } catch (final MalformedURLException e) {
            try {
                return new File(maybeUrl).toURI().toURL();
            } catch (final NullPointerException | MalformedURLException e1) {
                throw new IllegalStateException("Cannot load strategy " + maybeUrl, e1);
            }
        }
    }

    public static Refreshable<InvestmentStrategy> create(final String maybeUrl) {
        final Refreshable<InvestmentStrategy> result =
                new RefreshableInvestmentStrategy(RefreshableInvestmentStrategy.convertToUrl(maybeUrl));
        Scheduler.BACKGROUND_SCHEDULER.submit(result, Duration.ofHours(1));
        return result;
    }

    private final URL url;

    private RefreshableInvestmentStrategy(final URL target) {
        this.url = target;
    }

    @Override
    public Optional<Refreshable<?>> getDependedOn() {
        return Optional.empty();
    }

    @Override
    protected Supplier<Optional<String>> getLatestSource() {
        return () -> {
            try (final BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream(), Defaults.CHARSET))) {
                return Optional.of(r.lines().collect(Collectors.joining(System.lineSeparator())));
            } catch (final IOException ex) {
                LOGGER.warn("Failed reading strategy.", ex);
                return Optional.empty();
            }
        };
    }

    @Override
    protected Optional<InvestmentStrategy> transform(final String source) {
        return InvestmentStrategyLoader.load(source);
    }
}
