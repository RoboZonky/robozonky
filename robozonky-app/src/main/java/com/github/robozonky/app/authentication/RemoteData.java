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

package com.github.robozonky.app.authentication;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RemoteData {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteData.class);

    private final Wallet wallet;
    private final Statistics statistics;
    private final Map<Integer, Blocked> blocked;
    private final Map<Rating, BigDecimal> atRisk;

    private RemoteData(final Wallet wallet, final Statistics statistics, final Map<Integer, Blocked> blocked,
                       final Map<Rating, BigDecimal> atRisk) {
        this.wallet = wallet;
        this.statistics = statistics;
        this.blocked = blocked;
        this.atRisk = atRisk;
    }

    public static RemoteData load(final Tenant tenant) {
        LOGGER.debug("Loading the latest Zonky portfolio information.");
        final Map<Rating, BigDecimal> atRisk = Util.getAmountsAtRisk(tenant); // goes first as it will take some time
        final Statistics statistics = tenant.call(Zonky::getStatistics);
        final Map<Integer, Blocked> blocked = Util.readBlockedAmounts(tenant, statistics);
        final Wallet wallet = tenant.call(Zonky::getWallet); // goes last as it's a very short call
        LOGGER.debug("Finished.");
        return new RemoteData(wallet, statistics, blocked, atRisk);
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public Map<Integer, Blocked> getBlocked() {
        return Collections.unmodifiableMap(blocked);
    }

    public Map<Rating, BigDecimal> getAtRisk() {
        return Collections.unmodifiableMap(atRisk);
    }

    @Override
    public String toString() {
        return "RemoteData{" +
                "atRisk=" + atRisk +
                ", blocked=" + blocked +
                ", statistics=" + statistics +
                ", wallet=" + wallet +
                '}';
    }
}
