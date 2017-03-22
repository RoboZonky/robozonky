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

package com.github.triceo.robozonky.notifications.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.notifications.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will convert an investment into a timestamped file informing user of when the investment was made.
 */
abstract class AbstractFileStoringListener<T extends Event> implements EventListener<T> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    Optional<File> storeInvestmentMade(final File target, final int loanId, final int amount) {
        final Collection<String> output = Collections.singletonList("#" + loanId + ": " + amount + " CZK");
        try {
            Files.write(target.toPath(), output);
            return Optional.of(target);
        } catch (final IOException ex) {
            LOGGER.warn("Failed writing out {}.", target, ex);
            return Optional.empty();
        }
    }

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final Counter filesOfThisType;
    private final ListenerSpecificNotificationProperties properties;

    protected AbstractFileStoringListener(final ListenerSpecificNotificationProperties properties) {
        this.properties = properties;
        this.filesOfThisType = new Counter(this.getClass().getSimpleName(), properties.getListenerSpecificHourlyLimit());
    }

    boolean shouldStoreFile(final T event) {
        return this.properties.getGlobalCounter().allow() && this.filesOfThisType.allow();
    }

    abstract int getLoanId(final T event);

    abstract int getAmount(final T event);

    abstract String getSuffix(final T event);

    @Override
    public void handle(final T event) {
        if (!this.shouldStoreFile(event)) {
            LOGGER.debug("Will not store file.");
            return;
        }
        final Temporal now = event.getCreatedOn();
        final String filename = "robozonky." + AbstractFileStoringListener.FORMATTER.format(now) + '.'
                + this.getSuffix(event);
        this.storeInvestmentMade(new File(filename), this.getLoanId(event), this.getAmount(event)).ifPresent(f -> {
            this.properties.getGlobalCounter().increase();
            this.filesOfThisType.increase();
        });
    }

}
