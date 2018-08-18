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
import java.util.Queue;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.portfolio.Portfolio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates to state, which happen through {@link #getTenant()}, are postponed until {@link #run()} is called. Likewise
 * for events fired through {@link #fire(Event)}.
 */
public class Transactional implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Transactional.class);

    private final Portfolio portfolio;
    private final Tenant tenant;
    private final Queue<Event> eventsToFire = new LinkedList<>();
    private final Queue<Runnable> stateUpdates = new LinkedList<>();

    public Transactional(final Portfolio portfolio, final Tenant tenant) {
        this.portfolio = portfolio;
        this.tenant = new TransactionalTenant(this, tenant);
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    Queue<Runnable> getStateUpdates() {
        return stateUpdates;
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
}
