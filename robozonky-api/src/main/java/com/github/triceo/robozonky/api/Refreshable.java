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

package com.github.triceo.robozonky.api;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a resource that can be periodically checked for new results.
 *
 * The aim of this class is to be scheduled using a {@link ScheduledExecutorService}, while another thread is calling
 * {@link #getLatest()} to retrieve the latest version of the resource.
 *
 * @param <T> Type of the resource.
 */
public abstract class Refreshable<T> implements Runnable {

    /**
     * Create an instance of this class that will always return empty resource.
     *
     * @return The returned instance's {@link #getLatest()} will always return {@link Optional#empty()}.
     */
    public static Refreshable<Void> createImmutable() {
        return Refreshable.createImmutable(null);
    }

    /**
     * Create an instance of this class that will never refresh the given resource.
     *
     * @param toReturn Instance will always return this object.
     * @param <I> Type of object to return.
     * @return The returned instance will never change it's {@link #getLatest()}.
     */
    public static <I> Refreshable<I> createImmutable(final I toReturn) {
        return new Refreshable<I>() {
            @Override
            public Optional<Refreshable<?>> getDependedOn() {
                return Optional.empty();
            }

            @Override
            protected Optional<String> getLatestSource() {
                return Optional.of("");
            }

            @Override
            protected Optional<I> transform(final String source) {
                return Optional.ofNullable(toReturn);
            }
        };
    }

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final AtomicReference<String> latestKnownSource = new AtomicReference<>();
    private final AtomicReference<T> cachedResult = new AtomicReference<>();

    /**
     * Whether or not the refresh of this resource depends on the refresh of another resource. This method exists so
     * that any scheduler can properly include all the required {@link Refreshable}s.
     * @return Present if this resource needs another resource to be properly refreshed.
     */
    public abstract Optional<Refreshable<?>> getDependedOn();

    /**
     * Result of this method will be used as an identifier of the resource state. While {@link #run()} is being
     * executed, if the result of this method no longer {@link #equals(Object)} its value from previous call,
     * {@link #transform(String)} will be called, resulting in {@link #getLatest()} changing its return value.
     *
     * @return Identifier for the content. If empty, {@link #transform(String)} will not be called and
     * {@link #getLatest()} will become empty.
     */
    protected abstract Optional<String> getLatestSource();

    /**
     * Transform resource source into a new version of the resource. This method will be called when a fresh resource
     * is being requested.
     *
     * @param source The source to use when creating fresh instance of the resource.
     * @return The fresh version of the resource. Empty if source could not be parsed.
     */
    protected abstract Optional<T> transform(final String source);

    /**
     * Latest version of the resource.
     *
     * @return Empty if the source could not be parsed, or if {@link #run()} was not yet executed.
     */
    public Optional<T> getLatest() {
        return Optional.ofNullable(cachedResult.get());
    }

    /**
     * Update the value of {@link #getLatest()}, based on whether {@link #getLatestSource()} indicates there were any
     * changes in the resource.
     */
    @Override
    public void run() {
        final Optional<String> maybeNewSource = this.getLatestSource();
        if (maybeNewSource.isPresent()) {
            final String newSource = maybeNewSource.get();
            if (Objects.equals(newSource, latestKnownSource.get())) {
                return;
            }
            LOGGER.trace("New source found.");
            // source changed, result needs to be refreshed
            latestKnownSource.set(newSource);
            final Optional<T> maybeNewResult = transform(newSource);
            if (maybeNewResult.isPresent()) {
                final T newResult = maybeNewResult.get();
                LOGGER.debug("Source successfully transformed to {}.", newResult.getClass());
                cachedResult.set(newResult);
            } else {
                cachedResult.set(null);
            }
        } else {
            this.latestKnownSource.set(null);
            final T oldResult = this.cachedResult.getAndSet(null);
            if (oldResult != null) {
                LOGGER.debug("Result reset.");
            }
        }
    }

}
