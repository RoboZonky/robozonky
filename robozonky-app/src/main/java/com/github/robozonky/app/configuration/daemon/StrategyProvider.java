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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.common.extensions.StrategyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StrategyProvider implements Refreshable.RefreshListener<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyProvider.class);

    private final AtomicReference<InvestmentStrategy> toInvest = new AtomicReference<>();
    private final AtomicReference<SellStrategy> toSell = new AtomicReference<>();
    private final AtomicReference<PurchaseStrategy> toPurchase = new AtomicReference<>();

    @Override
    public void valueSet(final String newValue) {
        LOGGER.trace("Loading strategies.");
        toInvest.set(StrategyLoader.toInvest(newValue).orElse(null));
        toPurchase.set(StrategyLoader.toPurchase(newValue).orElse(null));
        toSell.set(StrategyLoader.toSell(newValue).orElse(null));
        LOGGER.trace("Finished.");
    }

    @Override
    public void valueUnset(final String oldValue) {
        Stream.of(toInvest, toSell, toPurchase).forEach(ref -> ref.set(null));
        LOGGER.trace("Removed all strategies.");
    }

    @Override
    public void valueChanged(final String oldValue, final String newValue) {
        valueSet(newValue);
    }

    public Optional<InvestmentStrategy> getToInvest() {
        return Optional.ofNullable(toInvest.get());
    }

    public Optional<SellStrategy> getToSell() {
        return Optional.ofNullable(toSell.get());
    }

    public Optional<PurchaseStrategy> getToPurchase() {
        return Optional.ofNullable(toPurchase.get());
    }
}
