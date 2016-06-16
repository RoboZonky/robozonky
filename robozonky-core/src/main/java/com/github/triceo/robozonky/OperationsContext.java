/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky;

import java.math.BigDecimal;

import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZotifyApi;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;

/**
 * Carries information to enable {@link Operations} to function, such as an authenticated Zonky API.
 */
public class OperationsContext {

    private final ZonkyApi zonkyApi;
    private final ZotifyApi zotifyApi;
    private final InvestmentStrategy strategy;
    private final boolean dryRun;
    private final BigDecimal dryRunInitialBalance;

    public OperationsContext(final ZonkyApi authenticated, final ZotifyApi cached, final InvestmentStrategy strategy,
                             final boolean dryRun, final int dryRunInitialBalance) {
        this.zonkyApi = authenticated;
        this.zotifyApi = cached;
        this.strategy = strategy;
        this.dryRun = dryRun;
        this.dryRunInitialBalance = BigDecimal.valueOf(dryRunInitialBalance);
    }

    protected ZonkyApi getZonkyApi() {
        return this.zonkyApi;
    }

    protected ZotifyApi getZotifyApi() {
        return this.zotifyApi;
    }

    public InvestmentStrategy getStrategy() {
        return this.strategy;
    }

    public boolean isDryRun() {
        return this.dryRun;
    }

    public BigDecimal getDryRunInitialBalance() {
        return this.dryRunInitialBalance;
    }

}
