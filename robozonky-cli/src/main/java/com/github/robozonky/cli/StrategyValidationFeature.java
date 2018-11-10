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

package com.github.robozonky.cli;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.LongAdder;

import com.github.robozonky.common.extensions.StrategyLoader;
import com.github.robozonky.internal.api.Defaults;
import org.apache.commons.io.IOUtils;
import picocli.CommandLine;

@CommandLine.Command(name = "strategy-validator", description = StrategyValidationFeature.DESCRIPTION)
public final class StrategyValidationFeature extends AbstractFeature {

    static final String DESCRIPTION = "Validate a strategy file.";

    @CommandLine.Option(names = {"-l", "--location"}, description = "URL leading to the strategy.", required = true)
    private URL location;
    private String text;
    private LongAdder adder = new LongAdder();

    public StrategyValidationFeature(final URL location) {
        this.location = location;
    }

    public StrategyValidationFeature(final File location) throws MalformedURLException {
        this(location.toURI().toURL());
    }

    StrategyValidationFeature(final File location, final LongAdder adder) throws MalformedURLException {
        this(location);
        this.adder = adder;
    }

    StrategyValidationFeature() {
        // for Picocli
        this.adder = new LongAdder();
    }

    @Override
    public String describe() {
        return DESCRIPTION;
    }

    @Override
    public void setup() throws SetupFailedException {
        try {
            text = IOUtils.toString(location, Defaults.CHARSET);
        } catch (final IOException ex) {
            throw new SetupFailedException(ex);
        }
    }

    private void report(final LongAdder adder, final String type) {
        adder.increment();
        LOGGER.info("{} strategy present.", type);
    }

    @Override
    public void test() throws TestFailedException {
        adder.reset();
        StrategyLoader.toInvest(text).ifPresent(s -> report(adder, "Investing"));
        StrategyLoader.toPurchase(text).ifPresent(s -> report(adder, "Purchasing"));
        StrategyLoader.toSell(text).ifPresent(s -> report(adder, "Selling"));
        if (adder.sum() == 0) {
            throw new TestFailedException("No strategies found. Check log for possible parser errors.");
        }
    }
}
