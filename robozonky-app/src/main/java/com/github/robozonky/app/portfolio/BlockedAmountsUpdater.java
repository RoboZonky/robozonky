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

class BlockedAmountsUpdater extends Refreshable<OffsetDateTime> {

    private final Authenticated authenticated;

    public BlockedAmountsUpdater(final Authenticated authenticated) {
        this.authenticated = authenticated;
    }

    @Override
    protected Supplier<Optional<String>> getLatestSource() {
        return () -> Optional.of(UUID.randomUUID().toString()); // update every time
    }

    @Override
    protected Optional<OffsetDateTime> transform(final String source) {
        authenticated.run(BlockedAmounts.INSTANCE);
        return Optional.of(OffsetDateTime.now());
    }
}
