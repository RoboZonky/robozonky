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

package com.github.triceo.robozonky.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.events.EventListener;
import com.github.triceo.robozonky.api.events.EventRegistry;
import com.github.triceo.robozonky.api.events.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will convert an investment into a timestamped file informing user of when the investment was made.
 */
class InvestmentReportingListener implements EventListener<InvestmentMadeEvent>, State.Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvestmentReportingListener.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    static final String SUFFIX_DRY_RUN = "dry";
    static final String SUFFIX_REAL_RUN = "invested";

    private static Optional<File> storeInvestmentMade(final File target, final Investment i) {
        final Collection<String> output =
                Collections.singletonList("#" + i.getLoanId() + ": " + i.getAmount() + " CZK");
        try {
            Files.write(target.toPath(), output);
            InvestmentReportingListener.LOGGER.info("Investment #{} stored in file '{}'.", i.getLoanId(),
                    target.getAbsolutePath());
            return Optional.of(target);
        } catch (final IOException ex) {
            InvestmentReportingListener.LOGGER.warn("Failed writing out the investment #{}", i.getLoanId(), ex);
            return Optional.empty();
        }
    }

    private final boolean isDryRun;

    public InvestmentReportingListener(final boolean isDryRun) {
        this.isDryRun = isDryRun;
    }

    @Override
    public void handle(final InvestmentMadeEvent event) {
        final String suffix = this.isDryRun ? InvestmentReportingListener.SUFFIX_DRY_RUN
                : InvestmentReportingListener.SUFFIX_REAL_RUN;
        final Temporal now = OffsetDateTime.now();
        final String filename = "robozonky." + InvestmentReportingListener.FORMATTER.format(now) + '.' + suffix;
        InvestmentReportingListener.storeInvestmentMade(new File(filename), event.getInvestment());
    }

    @Override
    public Optional<Consumer<ReturnCode>> get() { // register as app state handler
        EventRegistry.INSTANCE.addListener(InvestmentMadeEvent.class, this);
        return Optional.of((code) -> EventRegistry.INSTANCE.removeListener(InvestmentMadeEvent.class, this));
    }
}
