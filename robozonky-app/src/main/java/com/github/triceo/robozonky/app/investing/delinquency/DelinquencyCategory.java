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

package com.github.triceo.robozonky.app.investing.delinquency;

import java.util.Collection;
import java.util.function.Supplier;

import com.github.triceo.robozonky.api.remote.entities.Investment;

public enum DelinquencyCategory {

    MILD(KnownDeliquents10Plus::new),
    SEVERE(KnownDeliquents30Plus::new),
    CRITICAL(KnownDeliquents60Plus::new),
    DEFAULTED(KnownDeliquents90Plus::new);

    private final Supplier<KnownDelinquents> supplier;

    DelinquencyCategory(final Supplier<KnownDelinquents> supplier) {
        this.supplier = supplier;
    }

    public Collection<Delinquent> getKnownDelinquents() {
        return supplier.get().get();
    }

    public void updateKnownDelinquents(final Collection<Delinquent> presentDelinquents) {
        supplier.get().update(presentDelinquents);
    }

    public void purge(final Collection<Investment> investmentsToPurge) {
        supplier.get().purge(investmentsToPurge);
    }

}
