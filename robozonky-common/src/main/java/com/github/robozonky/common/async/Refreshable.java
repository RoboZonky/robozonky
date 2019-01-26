/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.common.async;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a resource that can be periodically checked for new results.
 * <p>
 * The aim of this class is to be scheduled using a {@link ScheduledExecutorService}, while another thread is reading
 * the latest result. Preferred use is through {@link Refreshable.RefreshListener} as registered via
 * {@link Refreshable#Refreshable(Refreshable.RefreshListener[])}. Alternatively, the latest result is also available
 * via {@link #get()}.
 * <p>
 * Only use this class if you need to periodically refresh a given remote resource and have the latest version of the
 * resource available. If you're trying to do anything with this class other than simple unconditional reads using the
 * {@link #get()} method, you are most likely abusing this class and will eventually pay the price.
 * @param <T> Type of the resource.
 */
public abstract class Refreshable<T> implements Runnable,
                                                Supplier<Optional<T>> {

    protected final Logger logger = LogManager.getLogger(this.getClass());
    private final String id;
    private final AtomicReference<String> latestKnownSource = new AtomicReference<>();
    private final AtomicReference<T> cachedResult = new AtomicReference<>();
    private final Collection<Refreshable.RefreshListener<T>> listeners = new CopyOnWriteArraySet<>();

    @SafeVarargs
    protected Refreshable(final Refreshable.RefreshListener<T>... listeners) {
        this(UUID.randomUUID().toString(), listeners);
    }

    @SafeVarargs
    protected Refreshable(final String id, final Refreshable.RefreshListener<T>... listeners) {
        this.id = id;
        this.registerListener(new UpdateNotification());
        for (final Refreshable.RefreshListener<T> l : listeners) {
            this.registerListener(l);
        }
    }

    /**
     * Result of this method will be used to fetch the latest resource state. While {@link #run()} is being
     * executed, if the result of the call no longer {@link #equals(Object)} its value from previous call,
     * {@link #transform(String)} will be called, resulting in {@link #get()} changing its return value.
     * @return The latest state of the resource.
     * @throws Exception When the resource could not be fetched for whatever reason.
     */
    protected abstract String getLatestSource() throws Exception;

    /**
     * Transform resource source into a new version of the resource. This method will be called when a fresh resource
     * is being requested.
     * @param source The source to use when creating fresh instance of the resource.
     * @return The fresh version of the resource. Empty if source could not be parsed.
     */
    protected abstract Optional<T> transform(final String source);

    /**
     * Latest version of the resource.
     * @return Empty if the source could not be parsed.
     */
    @Override
    public Optional<T> get() {
        return Optional.ofNullable(cachedResult.get());
    }

    /**
     * Register an object to listen for changes to {@link #get()}.
     * @param listener Listener to register.
     * @return False if already registered.
     */
    public boolean registerListener(final Refreshable.RefreshListener<T> listener) {
        final boolean added = this.listeners.add(listener);
        if (!added) {
            return false;
        }
        get().ifPresent(listener::valueSet);
        return true;
    }

    /**
     * Unregister a listener previously registered through {@link #registerListener(Refreshable.RefreshListener)}.
     * @param listener Listener to unregister.
     * @return False if not registered before.
     */
    public boolean unregisterListener(final Refreshable.RefreshListener<T> listener) {
        final boolean removed = this.listeners.remove(listener);
        if (!removed) {
            return false;
        }
        get().ifPresent(listener::valueUnset);
        return true;
    }

    private void storeResult(final T result) {
        final T previous = cachedResult.getAndSet(result);
        if (Objects.equals(previous, result)) { // both values equal or null
            logger.trace("Value not changed: {}.", this);
            return;
        }
        if (previous == null) { // value newly available
            this.listeners.forEach(l -> l.valueSet(result));
        } else if (result == null) { // value lost
            this.listeners.forEach(l -> l.valueUnset(previous));
        } else { // value changed
            this.listeners.forEach(l -> l.valueChanged(previous, result));
        }
    }

    private Optional<String> getSource() {
        try {
            return Optional.ofNullable(this.getLatestSource());
        } catch (final Exception ex) {
            logger.warn("Failed reading resource.", ex);
            return Optional.empty();
        }
    }

    private void runLocked() {
        final Optional<String> maybeNewSource = this.getSource();
        if (maybeNewSource.isPresent()) {
            final String newSource = maybeNewSource.get();
            if (Objects.equals(newSource, latestKnownSource.get())) {
                logger.trace("Source not changed: {}.", this);
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
     * Update the value of {@link #get()}, based on whether {@link #getLatestSource()} indicates there were any
     * changes in the resource.
     */
    @Override
    public void run() {
        try {
            logger.trace("Starting {}.", this);
            runLocked();
            logger.trace("Finished {}.", this);
        } catch (final Exception ex) {
            logger.debug("Refresh failed: {}.", this, ex);
        }
    }

    @Override
    public final String toString() {
        return this.getClass().getSimpleName() + "{id='" + id + "'}";
    }

    /**
     * Listener for changes to the original resource. Use {@link #registerListener(Refreshable.RefreshListener)} to
     * enable. Implementations of methods in this interface must not throw exceptions.
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
            valueSet(newValue);
        }
    }

    private final class UpdateNotification implements Refreshable.RefreshListener<T> {

        @Override
        public void valueSet(final T newValue) {
            logger.trace("New value '{}': {}.", newValue, this);
        }

        @Override
        public void valueUnset(final T oldValue) {
            logger.trace("Value removed: {}.", this);
        }

        @Override
        public void valueChanged(final T oldValue, final T newValue) {
            logger.trace("Value changed to '{}': {}.", newValue, this);
        }
    }
}
