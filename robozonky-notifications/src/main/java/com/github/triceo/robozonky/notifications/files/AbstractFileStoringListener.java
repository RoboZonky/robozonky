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

package com.github.triceo.robozonky.notifications.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will convert an investment into a timestamped file informing user of when the investment was made.
 */
abstract class AbstractFileStoringListener<T extends Event> implements EventListener<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFileStoringListener.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    static Optional<File> storeInvestmentMade(final File target, final int loanId, final int amount) {
        final Collection<String> output = Collections.singletonList("#" + loanId + ": " + amount + " CZK");
        try {
            Files.write(target.toPath(), output);
            return Optional.of(target);
        } catch (final IOException ex) {
            AbstractFileStoringListener.LOGGER.warn("Failed writing out {}.", target, ex);
            return Optional.empty();
        }
    }

    abstract int getLoanId(final T event);

    abstract int getAmount(final T event);

    abstract String getSuffix();

    @Override
    public void handle(final T event) {
        final Temporal now = OffsetDateTime.now();
        final String filename = "robozonky." + AbstractFileStoringListener.FORMATTER.format(now) + '.'
                + this.getSuffix();
        AbstractFileStoringListener.storeInvestmentMade(new File(filename), this.getLoanId(event),
                this.getAmount(event));
    }

}
