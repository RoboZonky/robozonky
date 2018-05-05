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

package com.github.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.util.Refreshable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RemoteBalanceImpl implements RemoteBalance,
                                   Refreshable.RefreshListener<BigDecimal> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteBalanceImpl.class);

    private final Refreshable<BigDecimal> provider;
    private final boolean isDryRun;
    private final BigDecimal dryRunMinimum = BigDecimal.valueOf(Settings.INSTANCE.getDryRunBalanceMinimum());
    private final AtomicReference<BigDecimal> latest = new AtomicReference<>(BigDecimal.valueOf(Integer.MIN_VALUE));

    public RemoteBalanceImpl(final Refreshable<BigDecimal> provider, final boolean isDryRun) {
        this.isDryRun = isDryRun;
        this.provider = provider;
        provider.registerListener(this);
    }

    @Override
    public void update(final BigDecimal change) {
        provider.run();
        if (isDryRun) {
            latest.accumulateAndGet(change, BigDecimal::add);
            LOGGER.debug("Locally added {} CZK, is {} CZK.", change, latest.get());
        }
    }

    @Override
    public BigDecimal get() {
        if (isDryRun && dryRunMinimum.compareTo(BigDecimal.ZERO) >= 0) {
            return latest.get().max(dryRunMinimum);
        } else {
            return latest.get();
        }
    }

    @Override
    public void valueSet(final BigDecimal newValue) {
        latest.set(newValue);
        LOGGER.debug("Set to {} CZK.", newValue);
    }

    @Override
    public void valueUnset(final BigDecimal oldValue) {
        // don't do anything
    }

    @Override
    public void valueChanged(final BigDecimal oldValue, final BigDecimal newValue) {
        final BigDecimal difference = newValue.subtract(oldValue);
        latest.accumulateAndGet(difference, BigDecimal::add);
        LOGGER.debug("Remotely added {} CZK, is {} CZK.", difference, latest.get());
    }
}
