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

package com.github.robozonky.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Expiring<T> implements Supplier<Optional<T>> {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final AtomicReference<Tuple<T>> value = new AtomicReference<>();
    private final Duration expireAfter;
    private final Runnable runWhenUpdated;

    protected Expiring(final Duration expireAfter, final Runnable runWhenUpdated) {
        this.expireAfter = expireAfter;
        this.runWhenUpdated = runWhenUpdated;
    }

    protected Expiring(final Duration expireAfter) {
        this(expireAfter, () -> {
            // do nothing
        });
    }

    protected abstract Optional<T> retrieve();

    @Override
    public Optional<T> get() {
        final Tuple<T> result = value.updateAndGet(old -> {
            if (old == null || !old.getRetrievedOn().plus(expireAfter).isAfter(Instant.now())) {
                LOGGER.trace("Retrieving new value.");
                return retrieve().map(v -> {
                    LOGGER.debug("Retrieved new value: {}.", v);
                    runWhenUpdated.run();
                    return new Tuple<>(v);
                }).orElse(old);
            } else {
                LOGGER.trace("Keeping existing value.");
                return old;
            }
        });
        return result == null ? Optional.empty() : Optional.ofNullable(result.getValue());
    }

    private static final class Tuple<X> {

        private final X value;
        private final Instant retrievedOn;

        public Tuple(final X value, final Instant retreivedOn) {
            this.value = value;
            this.retrievedOn = retreivedOn;
        }

        public Tuple(final X value) {
            this(value, Instant.now());
        }

        public X getValue() {
            return value;
        }

        public Instant getRetrievedOn() {
            return retrievedOn;
        }
    }

}
