/*
 * Copyright 2021 The RoboZonky Project
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

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationMode;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.async.ReloadListener;
import com.github.robozonky.internal.async.Reloadable;
import com.github.robozonky.internal.extensions.StrategyLoader;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.util.StringUtil;
import com.github.robozonky.internal.util.UrlUtil;

import io.micrometer.core.instrument.Timer;

class StrategyProvider implements ReloadListener<String> {

    private static final Logger LOGGER = LogManager.getLogger(StrategyProvider.class);
    private static final String TIMER_NAME = "robozonky.response";
    private static final String TIMER_KEY = "strategy";

    private final AtomicReference<String> lastLoadedStrategy = new AtomicReference<>();
    private final AtomicReference<InvestmentStrategy> toInvest = new AtomicReference<>();
    private final AtomicReference<SellStrategy> toSell = new AtomicReference<>();
    private final AtomicReference<PurchaseStrategy> toPurchase = new AtomicReference<>();
    private final AtomicReference<ReservationStrategy> forReservations = new AtomicReference<>();
    private final Reloadable<String> reloadableStrategy;

    private final Timer investingTimer = Timer.builder(TIMER_NAME)
        .tag(TIMER_KEY, "investing")
        .register(Defaults.METER_REGISTRY);
    private final Timer reservationTimer = Timer.builder(TIMER_NAME)
        .tag(TIMER_KEY, "reservations")
        .register(Defaults.METER_REGISTRY);
    private final Timer purchasingTimer = Timer.builder(TIMER_NAME)
        .tag(TIMER_KEY, "purchasing")
        .register(Defaults.METER_REGISTRY);
    private final Timer sellingTimer = Timer.builder(TIMER_NAME)
        .tag(TIMER_KEY, "selling")
        .register(Defaults.METER_REGISTRY);

    StrategyProvider(final String strategyLocation) {
        this.reloadableStrategy = Reloadable.with(() -> readStrategy(strategyLocation))
            .addListener(this)
            .reloadAfter(Duration.ofHours(1))
            .async()
            .build();
    }

    StrategyProvider() {
        this.reloadableStrategy = null;
    }

    private static String readStrategy(final String strategyLocation) {
        try (var inputStream = UrlUtil.open(UrlUtil.toURL(strategyLocation))
            .getInputStream()) {
            return StringUtil.toString(inputStream);
        } catch (Exception ex) {
            LOGGER.error("Failed reading strategy source.", ex);
            throw new IllegalStateException("Failed reading strategy source.", ex);
        }
    }

    public static StrategyProvider createFor(final String strategyLocation) {
        return new StrategyProvider(strategyLocation);
    }

    public static StrategyProvider empty() { // for testing purposes only
        return new StrategyProvider();
    }

    private static <T> T set(final AtomicReference<T> ref, final Supplier<Optional<T>> provider, final String desc) {
        final T value = ref.updateAndGet(old -> provider.get()
            .orElse(null));
        if (Objects.isNull(value)) {
            LOGGER.info("{} strategy inactive or missing, functionality disabled.", desc);
        } else {
            LOGGER.info("{} strategy correctly loaded.", desc);
        }
        return value;
    }

    @Override
    public void newValue(final String newValue) {
        var oldStrategy = lastLoadedStrategy.getAndSet(newValue);
        if (Objects.equals(oldStrategy, newValue)) {
            LOGGER.debug("No change in strategy source code detected.");
            return;
        }
        LOGGER.trace("Loading strategies.");
        var investStrategy = set(toInvest, () -> StrategyLoader.toInvest(newValue)
            .map(strategy -> (loanDescriptor, portfolioOverviewSupplier, sessionInfo) -> {
                // Decorate the freshly created strategy with a operation timer.
                var startInstant = DateUtil.now();
                var result = strategy.recommend(loanDescriptor, portfolioOverviewSupplier, sessionInfo);
                investingTimer.record(Duration.between(startInstant, DateUtil.now()));
                return result;
            }), "Primary marketplace investment");
        var purchaseStrategy = set(toPurchase, () -> StrategyLoader.toPurchase(newValue)
            .map(strategy -> (participationDescriptor, portfolioOverviewSupplier, sessionInfo) -> {
                // Decorate the freshly created strategy with a operation timer.
                var startInstant = DateUtil.now();
                var result = strategy.recommend(participationDescriptor, portfolioOverviewSupplier, sessionInfo);
                purchasingTimer.record(Duration.between(startInstant, DateUtil.now()));
                return result;
            }), "Secondary marketplace purchase");
        var sellingStrategy = set(toSell, () -> StrategyLoader.toSell(newValue)
            .map(strategy -> (investmentDescriptor, portfolioOverviewSupplier, sessionInfo) -> {
                // Decorate the freshly created strategy with a operation timer.
                var startInstant = DateUtil.now();
                var result = strategy.recommend(investmentDescriptor, portfolioOverviewSupplier, sessionInfo);
                sellingTimer.record(Duration.between(startInstant, DateUtil.now()));
                return result;
            }), "Portfolio selling");
        var reservationStrategy = set(forReservations, () -> StrategyLoader.forReservations(newValue)
            .map(strategy -> new ReservationStrategy() {
                @Override
                public ReservationMode getMode() {
                    return strategy.getMode();
                }

                @Override
                public boolean recommend(ReservationDescriptor reservationDescriptor,
                        Supplier<PortfolioOverview> portfolioOverviewSupplier,
                        SessionInfo sessionInfo) {
                    // Decorate the freshly created strategy with a operation timer.
                    var startInstant = DateUtil.now();
                    var result = strategy.recommend(reservationDescriptor, portfolioOverviewSupplier, sessionInfo);
                    reservationTimer.record(Duration.between(startInstant, DateUtil.now()));
                    return result;
                }
            }), "Loan reservation confirmation");
        var allStrategiesMissing = Stream.of(investStrategy, purchaseStrategy, sellingStrategy, reservationStrategy)
            .allMatch(Objects::isNull);
        if (allStrategiesMissing) {
            LOGGER.warn("No strategies are available, all operations are disabled. Check log for parser errors.");
        }
        LOGGER.trace("Finished.");
    }

    @Override
    public void valueUnset() {
        lastLoadedStrategy.set(null);
        Stream.of(toInvest, toSell, toPurchase, forReservations)
            .forEach(ref -> ref.set(null));
        LOGGER.warn("There are no strategies, all operations are disabled.");
    }

    private <T> T loadStrategy(Supplier<T> supplier) {
        if (reloadableStrategy != null) {
            reloadableStrategy.get();
        }
        return supplier.get();
    }

    public Optional<InvestmentStrategy> getToInvest() {
        return Optional.ofNullable(loadStrategy(toInvest::get));
    }

    public Optional<SellStrategy> getToSell() {
        return Optional.ofNullable(loadStrategy(toSell::get));
    }

    public Optional<PurchaseStrategy> getToPurchase() {
        return Optional.ofNullable(loadStrategy(toPurchase::get));
    }

    public Optional<ReservationStrategy> getForReservations() {
        return Optional.ofNullable(loadStrategy(forReservations::get));
    }
}
