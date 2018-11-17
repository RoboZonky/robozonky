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
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.common.RemoteBalance;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.util.Expiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RemoteBalanceImpl implements RemoteBalance {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteBalanceImpl.class);

    private final Expiring<Wallet> wallet;
    private final AtomicReference<BigDecimal> ephemeralUpdates = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> persistentUpdates = new AtomicReference<>(BigDecimal.ZERO);

    public RemoteBalanceImpl(final Tenant tenant) {
        this.wallet = new ExpiringWallet(tenant, () -> ephemeralUpdates.set(BigDecimal.ZERO));
    }

    @Override
    public synchronized void update(final BigDecimal change, final boolean clearWithNextReload) {
        LOGGER.debug("Requested update of {} CZK that {} ephemeral.", change, clearWithNextReload ? "is" : "is not");
        final AtomicReference<BigDecimal> toUpdate = clearWithNextReload ? ephemeralUpdates : persistentUpdates;
        toUpdate.updateAndGet(old -> old.add(change));
    }

    @Override
    public synchronized BigDecimal get() {
        final BigDecimal online = wallet.get().map(Wallet::getAvailableBalance).orElse(BigDecimal.ZERO);
        return online.add(ephemeralUpdates.get()).add(persistentUpdates.get());
    }

}
