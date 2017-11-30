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

package com.github.robozonky.app.configuration.daemon;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.portfolio.PortfolioDependant;

public class BlockedAmountsUpdater implements Runnable {

    private final Authenticated authenticated;
    private final Supplier<Optional<Portfolio>> portfolio;
    private final PortfolioDependant instance = new BlockedAmounts();

    public BlockedAmountsUpdater(final Authenticated authenticated, final Supplier<Optional<Portfolio>> portfolio) {
        this.authenticated = authenticated;
        this.portfolio = portfolio;
    }

    public PortfolioDependant getDependant() {
        return instance;
    }

    @Override
    public void run() {
        portfolio.get().ifPresent(folio -> instance.accept(folio, authenticated));
    }
}
