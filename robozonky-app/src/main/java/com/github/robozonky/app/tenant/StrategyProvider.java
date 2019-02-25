/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.tenant;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.common.async.Refreshable;
import com.github.robozonky.common.async.Tasks;
import com.github.robozonky.common.extensions.StrategyLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class StrategyProvider implements Refreshable.RefreshListener<String> {

    private static final Logger LOGGER = LogManager.getLogger(StrategyProvider.class);

    private final AtomicReference<InvestmentStrategy> toInvest = new AtomicReference<>();
    private final AtomicReference<SellStrategy> toSell = new AtomicReference<>();
    private final AtomicReference<PurchaseStrategy> toPurchase = new AtomicReference<>();
    private final AtomicReference<ReservationStrategy> forReservations = new AtomicReference<>();

    StrategyProvider() {
        // no external instances
    }

    public static Future<StrategyProvider> createFor(final String strategyLocation) {
        final RefreshableStrategy strategy = new RefreshableStrategy(strategyLocation);
        final StrategyProvider sp = new StrategyProvider(); // will always have the latest parsed strategies
        strategy.registerListener(sp);
        // start strategy refresh after the listener was registered
        return Tasks.SUPPORTING.scheduler().getExecutor().submit(() -> {
            // return a future; we only want to read the strategy provider when it's been initialized
            strategy.run();
            return sp;
        });
    }

    public static StrategyProvider empty() { // for testing purposes only
        return new StrategyProvider();
    }

    private static <T> T set(final AtomicReference<T> ref, final Supplier<Optional<T>> provider, final String desc) {
        final T value = ref.updateAndGet(old -> provider.get().orElse(null));
        final String log = Objects.isNull(value) ?
                "{} strategy inactive or missing, disabling all such operations." :
                "{} strategy correctly loaded.";
        LOGGER.info(log, desc);
        return value;
    }

    @Override
    public void valueSet(final String newValue) {
        LOGGER.trace("Loading strategies.");
        final InvestmentStrategy i = set(toInvest, () -> StrategyLoader.toInvest(newValue), "Investing");
        final PurchaseStrategy p = set(toPurchase, () -> StrategyLoader.toPurchase(newValue), "Purchasing");
        final SellStrategy s = set(toSell, () -> StrategyLoader.toSell(newValue), "Selling");
        final ReservationStrategy r = set(forReservations, () -> StrategyLoader.forReservations(newValue),
                                          "Reservations");
        final boolean allMissing = Stream.of(i, p, s, r).allMatch(Objects::isNull);
        if (allMissing) {
            LOGGER.warn("No strategies are available. Check log for parser errors.");
        }
        LOGGER.trace("Finished.");
    }

    @Override
    public void valueUnset(final String oldValue) {
        Stream.of(toInvest, toSell, toPurchase, forReservations).forEach(ref -> ref.set(null));
        LOGGER.warn("There are no strategies, all operations are disabled.");
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

    public Optional<ReservationStrategy> getForReservations() {
        return Optional.ofNullable(forReservations.get());
    }
}
