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

package com.github.triceo.robozonky.app.portfolio;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.app.authentication.Authenticated;

public class InvestmentUpdater extends Refreshable<OffsetDateTime> {

    private final Authenticated authenticated;

    public InvestmentUpdater(final Authenticated authenticated) {
        this.authenticated = authenticated;
    }

    /**
     * Will only update once a day, after 4am. This is to make sure that the updates happen when all the overnight
     * transactions on Zonky have cleared.
     * @return
     */
    @Override
    protected Supplier<Optional<String>> getLatestSource() {
        return () -> Optional.of(Util.getYesterdayIfAfter(Duration.ofHours(4)).toString());
    }

    @Override
    protected Optional<OffsetDateTime> transform(final String source) {
        authenticated.run(Investments.INSTANCE::update);
        return Optional.of(OffsetDateTime.now());
    }
}
