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

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.portfolio.Portfolio;

/**
 * Updates to state, which happen through {@link #getTenant()}, are postponed until {@link #run()} is called. Likewise
 * for events fired through {@link #fire(Event)}.
 *
 * This class is thread-safe, since multiple threads may want to fire events and/or store state data at the same time.
 */
public class Transactional extends SimpleTransactional implements Runnable {

    private final Portfolio portfolio;

    public Transactional(final Portfolio portfolio, final Tenant tenant) {
        super(tenant);
        this.portfolio = portfolio;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

}
