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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.util.function.Consumer;

import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.util.Refreshable;

class RemoteBalanceImpl implements RemoteBalance,
                                   Refreshable.RefreshListener<BigDecimal> {

    private final Refreshable<BigDecimal> provider;
    private final boolean isDryRun;
    private final BigDecimal dryRunMinimum = BigDecimal.valueOf(Settings.INSTANCE.getDryRunBalanceMinimum());
    private final ValueTracker latest;
    private final Runnable closer;

    /**
     *
     * @param provider
     * @param isDryRun
     * @param changeListener
     * @param closer This will be called during {@link #close()} to make sure that any resources open during the setup
     * for this object have been freed when this object is no longer necessary.
     */
    public RemoteBalanceImpl(final Refreshable<BigDecimal> provider, final boolean isDryRun,
                             final Consumer<BigDecimal> changeListener, final Runnable closer) {
        this.isDryRun = isDryRun;
        this.provider = provider;
        this.latest = new ValueTracker(BigDecimal.valueOf(Integer.MIN_VALUE), changeListener);
        this.closer = closer;
        provider.registerListener(this);
    }

    RemoteBalanceImpl(final Refreshable<BigDecimal> provider, final boolean isDryRun) {
        this(provider, isDryRun, b -> {
        }, () -> {
        });
    }

    @Override
    public void update(final BigDecimal change) {
        provider.run();
        if (isDryRun) {
            latest.add(change);
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
    }

    @Override
    public void valueUnset(final BigDecimal oldValue) {
        // don't do anything
    }

    @Override
    public void valueChanged(final BigDecimal oldValue, final BigDecimal newValue) {
        final BigDecimal difference = newValue.subtract(oldValue);
        latest.add(difference);
    }

    @Override
    public void close() {
        closer.run();
    }
}
