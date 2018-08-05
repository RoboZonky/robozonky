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

package com.github.robozonky.app.configuration.daemon;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.StateModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates to state, which happen through {@link #getTenant()}, are postponed until {@link #run()} is called. Likewise
 * for events fired through {@link #fire(Event)}.
 */
public final class Transactional implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Transactional.class);

    private final Portfolio portfolio;
    private final Tenant tenant;
    private final Queue<Event> eventsToFire = new LinkedList<>();
    private final Queue<Runnable> stateUpdates = new LinkedList<>();

    public Transactional(final Portfolio portfolio, final Tenant tenant) {
        this.portfolio = portfolio;
        this.tenant = new TransactionalTenant(tenant);
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    /**
     * Returns {@link TransactionalTenant} instead of the default implementation.
     * @return
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Stores event for future firing when {@link #run()}  is called.
     * @param event
     */
    public void fire(final Event event) {
        LOGGER.trace("Event stored within transaction: {}.", event);
        eventsToFire.add(event);
    }

    /**
     * Fire events and update state. Clears internal state, so that the next {@link #run()} call would not do anything
     * unless {@link #fire(Event)} or state updates are performed inbetween.
     */
    @Override
    public void run() {
        LOGGER.debug("Replaying transaction.");
        while (!stateUpdates.isEmpty()) {
            stateUpdates.poll().run();
        }
        while (!eventsToFire.isEmpty()) {
            Events.fire(eventsToFire.poll());
        }
    }

    /**
     * {@link #getState(Class)} returns {@link TransactionalInstanceState} instead of the default {@link InstanceState}
     * implementation. Every other method delegated to the default {@link Tenant} implementation.
     */
    private final class TransactionalTenant implements Tenant {

        private final Tenant parent;

        public TransactionalTenant(final Tenant parent) {
            this.parent = parent;
        }

        @Override
        public <T> T call(final Function<Zonky, T> operation) {
            return parent.call(operation);
        }

        @Override
        public Restrictions getRestrictions() {
            return parent.getRestrictions();
        }

        @Override
        public SessionInfo getSessionInfo() {
            return parent.getSessionInfo();
        }

        @Override
        public <T> InstanceState<T> getState(final Class<T> clz) {
            LOGGER.trace("Creating transactional instance state for {}.", clz);
            return new TransactionalInstanceState<>(parent.getState(clz));
        }
    }

    /**
     * Delegates to default {@link InstanceState} implementation, except for {@link #update(Consumer)} and
     * {@link #reset(Consumer)}, which are stored for later.
     * @param <T>
     */
    private final class TransactionalInstanceState<T> implements InstanceState<T> {

        private final InstanceState<T> parent;

        public TransactionalInstanceState(final InstanceState<T> parent) {
            this.parent = parent;
        }

        @Override
        public void update(final Consumer<StateModifier<T>> modifier) {
            LOGGER.debug("Updating transactional instance state for {}.", parent);
            stateUpdates.add(() -> parent.update(modifier));
        }

        @Override
        public void reset(final Consumer<StateModifier<T>> setter) {
            LOGGER.debug("Resetting transactional instance state for {}.", parent);
            stateUpdates.add(() -> parent.reset(setter));
        }

        @Override
        public Optional<String> getValue(final String key) {
            return parent.getValue(key);
        }

        @Override
        public Stream<String> getKeys() {
            return parent.getKeys();
        }
    }
}
