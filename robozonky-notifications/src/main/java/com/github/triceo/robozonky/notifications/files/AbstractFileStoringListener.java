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
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.SessionInfo;
import com.github.triceo.robozonky.notifications.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will convert an investment into a timestamped file informing user of when the investment was made.
 */
abstract class AbstractFileStoringListener<T extends Event> implements EventListener<T> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    void storeInvestmentMade(final File target, final int loanId, final int amount) throws IOException {
        final Collection<String> output = Collections.singletonList("#" + loanId + ": " + amount + " CZK");
        Files.write(target.toPath(), output);
    }

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final Counter filesOfThisType;
    private final ListenerSpecificNotificationProperties properties;

    protected AbstractFileStoringListener(final ListenerSpecificNotificationProperties properties) {
        this.properties = properties;
        this.filesOfThisType = new Counter(this.getClass().getSimpleName(),
                properties.getListenerSpecificHourlyLimit());
    }

    public Counter getFilesOfThisType() {
        return filesOfThisType;
    }

    public ListenerSpecificNotificationProperties getProperties() {
        return properties;
    }

    private Stream<Counter> getCounters() {
        return Stream.of(this.properties.getGlobalCounter(), this.filesOfThisType);
    }

    abstract int getLoanId(final T event);

    abstract int getAmount(final T event);

    abstract String getSuffix(final T event);

    @Override
    public void handle(final T event, final SessionInfo sessionInfo) {
        if (!this.getCounters().allMatch(Counter::allow)) { // hourly limit triggered
            LOGGER.debug("Will not store file.");
            return;
        }
        final Temporal now = event.getCreatedOn();
        final String filename =
                "robozonky." + AbstractFileStoringListener.FORMATTER.format(now) + '.' + this.getSuffix(event);
        try {
            this.storeInvestmentMade(new File(filename), this.getLoanId(event), this.getAmount(event));
            this.getCounters().forEach(Counter::increase);
        } catch (final IOException ex) { // notification not written; nothing much to do about it
            LOGGER.warn("Failed writing out {}.", filename, ex);
        }
    }

}
