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

package com.github.triceo.robozonky.marketplaces;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.marketplaces.ExpectedTreatment;
import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.api.remote.Api;
import com.github.triceo.robozonky.api.remote.entities.Loan;

public class ZotifyMarketplace implements Marketplace {

    private final Collection<Consumer<Collection<Loan>>> loanListeners = new LinkedHashSet<>();
    private final MarketplaceApiProvider apis = new MarketplaceApiProvider();
    private final Api api = apis.zotify();

    @Override
    public boolean registerListener(final Consumer<Collection<Loan>> listener) {
        return this.loanListeners.add(listener);
    }

    @Override
    public ExpectedTreatment specifyExpectedTreatment() {
        return ExpectedTreatment.POLLING;
    }

    @Override
    public void run() {
        final Collection<Loan> loans = api.getLoans();
        loanListeners.forEach(l -> l.accept(loans));
    }

    @Override
    public void close() {
        apis.close();
    }
}
