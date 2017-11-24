/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.api;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.internal.api.Retriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a resource that can be periodically checked for new results.
 * <p>
 * The aim of this class is to be scheduled using a {@link ScheduledExecutorService}, while another thread is calling
 * {@link #getLatest()} to retrieve the latest version of the resource.
 * <p>
 * Only use this class if you need to periodically refresh a given remote resource and have the latest version of the
 * resource available as a variable. Using this class for any other background checks will bring needless complexity
 * that could be avoided by just scheduling a {@link Runnable} through {@link ScheduledExecutorService}.
 * @param <T> Type of the resource.
 */
public abstract class Refreshable<T> implements Runnable {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final AtomicReference<String> latestKnownSource = new AtomicReference<>();
    private final AtomicReference<T> cachedResult = new AtomicReference<>();
    private final AtomicInteger requestsToPause = new AtomicInteger(0);
    private final AtomicBoolean refreshRequestedWhilePaused = new AtomicBoolean(false);
    /**
     * Will be used to prevent {@link #getLatest()} from returning before {@link #run()} fetched a value once.
     */
    private final CountDownLatch completionAssurance = new CountDownLatch(1);
    private final Collection<Refreshable.RefreshListener<T>> listeners = new CopyOnWriteArraySet<>();

    public Refreshable() {
        this.registerListener(new UpdateNotification()); // log changes to resource
    }

    /**
     * Create an instance of this class that will always return empty resource.
     * @return The returned instance's {@link #getLatest()} will always return {@link Optional#empty()}.
     */
    public static Refreshable<Void> createImmutable() {
        return Refreshable.createImmutable(null);
    }

    /**
     * Create an instance of this class that will never refresh the given resource.
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

            @Override
            public String toString() {
                return "ImmutableRefreshable{toReturn=" + toReturn + "}";
            }
        };
    }

    /**
     * Result of this method will be used to fetch the latest resource state. While {@link #run()} is being
     * executed, if the result of the call no longer {@link #equals(Object)} its value from previous call,
     * {@link #transform(String)} will be called, resulting in {@link #getLatest()} changing its return value.
     * <p>
     * The result of this method will be treated as a blocking operation, assuming it contains I/O calls.
     * @return Method to retrieve identifier for the content. If empty, {@link #transform(String)} will not be called
     * and {@link #getLatest()} will become empty.
     */
    protected abstract Supplier<Optional<String>> getLatestSource();

    /**
     * Transform resource source into a new version of the resource. This method will be called when a fresh resource
     * is being requested.
     * @param source The source to use when creating fresh instance of the resource.
     * @return The fresh version of the resource. Empty if source could not be parsed.
     */
    protected abstract Optional<T> transform(final String source);

    /**
     * Will block until {@link #run()} has finished at least once or until time runs out.
     * @param waitFor How long to wait for.
     * @return Empty if the source could not be parsed, if wait timed out or if the wait operation was interrupted.
     */
    public Optional<T> getLatest(final Duration waitFor) {
        try {
            final long millisToWaitFor = waitFor.toMillis();
            LOGGER.trace("Waiting for up to {} millis: {}.", millisToWaitFor, this);
            if (completionAssurance.await(millisToWaitFor, TimeUnit.MILLISECONDS)) {
                return Optional.ofNullable(cachedResult.get());
            } else {
                LOGGER.debug("Acquire failed: {}.", this);
                return Optional.empty();
            }
        } catch (final InterruptedException ex) {
            LOGGER.debug("Wait interrupted: {} (Reason: {}).", this, ex.getMessage());
            return Optional.empty();
        } finally {
            LOGGER.trace("Wait over: {}.", this);
        }
    }

    /**
     * Latest version of the resource. Will block until {@link #run()} has finished at least once.
     * @return Empty if the source could not be parsed or if the wait operation was interrupted.
     */
    public Optional<T> getLatest() {
        return getLatest(Duration.ofMillis(Long.MAX_VALUE));
    }

    /**
     * Register an object to listen for changes to {@link #getLatest()}.
     * @param listener Listener to register.
     * @return False if already registered.
     */
    public boolean registerListener(final Refreshable.RefreshListener<T> listener) {
        return this.listeners.add(listener);
    }

    /**
     * Unregister a listener previously registered through {@link #registerListener(Refreshable.RefreshListener)}.
     * @param listener Listener to unregister.
     * @return False if not registered before.
     */
    public boolean unregisterListener(final Refreshable.RefreshListener<T> listener) {
        return this.listeners.remove(listener);
    }

    /**
     * Will stop refreshing until the operation in question finishes. If multiple pause requests are active at the same
     * time, both must finished before the refresh is started again.
     * @param operation Operation to execute while the refreshable is paused.
     * @return Result of the operation.
     */
    public <X> X pauseFor(final Function<Refreshable<T>, X> operation) {
        requestsToPause.incrementAndGet();
        try {
            LOGGER.trace("Pausing: {}.", this);
            return operation.apply(this);
        } finally {
            LOGGER.trace("Releasing: {}.", this);
            final boolean isPaused = requestsToPause.decrementAndGet() > 0;
            final boolean needsRefresh = refreshRequestedWhilePaused.getAndSet(false);
            if (!isPaused && needsRefresh) {
                LOGGER.trace("Executing in lieu: {}.", this);
                run();
            }
        }
    }

    public boolean isPaused() {
        return requestsToPause.get() > 0;
    }

    private void storeResult(final T result) {
        final T previous = cachedResult.getAndSet(result);
        if (Objects.equals(previous, result)) {
            LOGGER.trace("Value not changed: {}.", this);
            return;
        }
        if (previous == null && result != null) { // value newly available
            this.listeners.forEach(l -> l.valueSet(result));
        } else if (previous != null && result == null) { // value lost
            this.listeners.forEach(l -> l.valueUnset(previous));
        } else { // value changed
            this.listeners.forEach(l -> l.valueChanged(previous, result));
        }
    }

    private synchronized void runLocked() {
        final Optional<String> maybeNewSource = Retriever.retrieve(this.getLatestSource());
        if (maybeNewSource.isPresent()) {
            final String newSource = maybeNewSource.get();
            if (Objects.equals(newSource, latestKnownSource.get())) {
                LOGGER.trace("Source not changed: {}.", this);
                return;
            }
            // source changed, result needs to be refreshed
            final Optional<T> maybeNewResult = transform(newSource);
            /*
             * only store new source if result actually refreshed, ie. did not throw; otherwise we're going to want to
             * try again next time.
             */
            latestKnownSource.set(newSource);
            // store result
            if (maybeNewResult.isPresent()) {
                final T newResult = maybeNewResult.get();
                storeResult(newResult);
            } else {
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
        if (requestsToPause.get() > 0) {
            refreshRequestedWhilePaused.set(true);
            LOGGER.trace("Paused, not refreshing: {}.", this);
            return;
        }
        try {
            LOGGER.trace("Starting {}.", this);
            runLocked();
        } catch (final Exception ex) {
            LOGGER.warn("Refresh failed: {}.", this, ex);
        } finally {
            completionAssurance.countDown();
            LOGGER.trace("Finished {}.", this);
        }
    }

    /**
     * Listener for changes to the original resource. Use {@link #registerListener(Refreshable.RefreshListener)} to
     * enable.
     * Implementations of methods in this interface must not throw exceptions.
     * @param <T> Target {@link Refreshable}'s generic type.
     */
    public interface RefreshListener<T> {

        /**
         * Resource now has a value where there was none before.
         * @param newValue New value for the resource.
         */
        default void valueSet(final T newValue) {
            // do nothing
        }

        /**
         * Resource used to have a value but no longer has one.
         * @param oldValue Former value of the resource.
         */
        default void valueUnset(final T oldValue) {
            // do nothing
        }

        /**
         * Resource continues to have a value, and that value has changed.
         * @param oldValue Former value of the resource.
         * @param newValue New value of the resource.
         */
        default void valueChanged(final T oldValue, final T newValue) {
            // do nothing
        }
    }

    private final class UpdateNotification implements Refreshable.RefreshListener<T> {

        @Override
        public void valueSet(final T newValue) {
            LOGGER.trace("New value '{}': {}.", newValue, this);
        }

        @Override
        public void valueUnset(final T oldValue) {
            LOGGER.trace("Value removed: {}.", this);
        }

        @Override
        public void valueChanged(final T oldValue, final T newValue) {
            LOGGER.trace("Value changed to '{}': {}.", newValue, this);
        }
    }
}
