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

package com.github.robozonky.app.portfolio;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.app.authentication.Authenticated;

public class BlockedAmountsUpdater extends Refreshable<OffsetDateTime> {

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
    protected Supplier<Optional<String>> getLatestSource() {
        return () -> Optional.of(UUID.randomUUID().toString()); // update every time
    }

    @Override
    protected Optional<OffsetDateTime> transform(final String source) {
        portfolio.get().ifPresent(folio -> authenticated.run(zonky -> getDependant().accept(folio, zonky)));
        return Optional.of(OffsetDateTime.now());
    }
}
