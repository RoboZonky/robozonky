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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.github.triceo.robozonky.internal.api.Retriever;
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
            protected Supplier<Optional<String>> getLatestSource() {
                return () -> Optional.of("");
            }

            @Override
            protected Optional<I> transform(final String source) {
                return Optional.ofNullable(toReturn);
            }

        };
    }

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final Semaphore valueIsMissing = new Semaphore(1);
    private final AtomicReference<String> latestKnownSource = new AtomicReference<>();
    private final AtomicReference<T> cachedResult = new AtomicReference<>();
    /**
     * Will be used to prevent {@link #getLatest()} from returning before {@link #run()} fetched a value once.
     */
    private final CountDownLatch completionAssurance = new CountDownLatch(1);

    public Refreshable() {
        this.valueIsMissing.acquireUninterruptibly();
    }

    /**
     * Whether or not the refresh of this resource depends on the refresh of another resource. This method exists so
     * that any scheduler can properly include all the required {@link Refreshable}s by overriding this one.
     * @return Present if this resource needs another resource to be properly refreshed.
     */
    public Optional<Refreshable<?>> getDependedOn() {
        return Optional.empty();
    }

    /**
     * Result of this method will be used to fetch the latest resource state. While {@link #run()} is being
     * executed, if the result of the call no longer {@link #equals(Object)} its value from previous call,
     * {@link #transform(String)} will be called, resulting in {@link #getLatest()} changing its return value.
     *
     * The result of this method will be treated as a blocking operation, assuming it contains I/O calls.
     *
     * @return Method to retrieve identifier for the content. If empty, {@link #transform(String)} will not be called
     * and {@link #getLatest()} will become empty.
     */
    protected abstract Supplier<Optional<String>> getLatestSource();

    /**
     * Transform resource source into a new version of the resource. This method will be called when a fresh resource
     * is being requested.
     *
     * @param source The source to use when creating fresh instance of the resource.
     * @return The fresh version of the resource. Empty if source could not be parsed.
     */
    protected abstract Optional<T> transform(final String source);

    /**
     * Will block until {@link #getLatest()} is able to return a non-empty result.
     * @return The return of {@link #getLatest()}'s optional.
     */
    public T getLatestBlocking() {
        try {
            valueIsMissing.acquireUninterruptibly();
            return cachedResult.get();
        } finally {
            valueIsMissing.release();
        }
    }

    /**
     * Latest version of the resource. Will block until {@link #run()} has finished at least once.
     *
     * @return Empty if the source could not be parsed or if the wait operation was interrupted.
     */
    public Optional<T> getLatest() {
        try {
            completionAssurance.await();
            return Optional.ofNullable(cachedResult.get());
        } catch (final InterruptedException e) {
            return Optional.empty();
        }
    }

    private void storeResult(final T result) {
        final T previous = cachedResult.getAndSet(result);
        if (Objects.equals(previous, result)) {
            return;
        }
        if (previous == null && result != null) { // value newly available
            LOGGER.debug("New value.");
            valueIsMissing.release();
        } else if (previous != null && result == null) { // value lost
            LOGGER.debug("No value.");
            valueIsMissing.acquireUninterruptibly();
        }
    }

    private void runLocked() {
        final Optional<String> maybeNewSource = Retriever.retrieve(this.getLatestSource());
        if (maybeNewSource.isPresent()) {
            final String newSource = maybeNewSource.get();
            if (Objects.equals(newSource, latestKnownSource.get())) {
                return;
            }
            // source changed, result needs to be refreshed
            latestKnownSource.set(newSource);
            final Optional<T> maybeNewResult = transform(newSource);
            if (maybeNewResult.isPresent()) {
                final T newResult = maybeNewResult.get();
                LOGGER.debug("Source successfully transformed to {}.", newResult.getClass());
                storeResult(newResult);
            } else {
                LOGGER.debug("Source not transformed.");
                storeResult(null);
            }
        } else {
            this.latestKnownSource.set(null);
            storeResult(null);
        }
    }

    /**
     * Update the value of {@link #getLatest()}, based on whether {@link #getLatestSource()} indicates there were any
     * changes in the resource.
     */
    @Override
    public void run() {
        try {
            runLocked();
        } finally {
            completionAssurance.countDown();
        }
    }

}
