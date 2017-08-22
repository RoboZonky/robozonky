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

package com.github.robozonky.marketplaces;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.common.remote.ApiProvider;

abstract class AbstractMarketplace implements Marketplace {

    private final Collection<Consumer<Collection<Loan>>> loanListeners = new LinkedHashSet<>();
    private final ApiProvider apis = this.api();

    protected abstract ApiProvider api();

    @Override
    public synchronized boolean registerListener(final Consumer<Collection<Loan>> listener) {
        return this.loanListeners.add(listener);
    }

    @Override
    public synchronized void run() {
        final Collection<Loan> loans = apis.marketplace();
        loanListeners.forEach(l -> l.accept(loans));
    }
}
