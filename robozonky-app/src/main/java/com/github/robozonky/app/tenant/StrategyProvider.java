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

import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.internal.async.ReloadListener;
import com.github.robozonky.internal.async.Reloadable;
import com.github.robozonky.internal.extensions.StrategyLoader;
import com.github.robozonky.internal.util.StringUtil;
import com.github.robozonky.internal.util.UrlUtil;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

class StrategyProvider implements ReloadListener<String> {

    private static final Logger LOGGER = LogManager.getLogger(StrategyProvider.class);

    private final AtomicReference<InvestmentStrategy> toInvest = new AtomicReference<>();
    private final AtomicReference<SellStrategy> toSell = new AtomicReference<>();
    private final AtomicReference<PurchaseStrategy> toPurchase = new AtomicReference<>();
    private final AtomicReference<ReservationStrategy> forReservations = new AtomicReference<>();
    private final Reloadable<String> reloadableStrategy;

    StrategyProvider(final String strategyLocation) {
        this.reloadableStrategy = Reloadable.with(() ->
                Try.withResources(() -> UrlUtil.open(UrlUtil.toURL(strategyLocation)))
                        .of(StringUtil::toString)
                        .getOrElseThrow((Function<Throwable, IllegalStateException>) IllegalStateException::new))
                .addListener(this)
                .reloadAfter(Duration.ofHours(1))
                .async()
                .build();
    }

    StrategyProvider() {
        this.reloadableStrategy = null;
    }

    public static StrategyProvider createFor(final String strategyLocation) {
        return new StrategyProvider(strategyLocation);
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
    public void newValue(final String newValue) {
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
    public void valueUnset() {
        Stream.of(toInvest, toSell, toPurchase, forReservations).forEach(ref -> ref.set(null));
        LOGGER.warn("There are no strategies, all operations are disabled.");
    }

    private void possiblyReloadStrategy() {
        if (reloadableStrategy != null) {
            reloadableStrategy.get();
        }
    }

    public Optional<InvestmentStrategy> getToInvest() {
        possiblyReloadStrategy();
        return Optional.ofNullable(toInvest.get());
    }

    public Optional<SellStrategy> getToSell() {
        possiblyReloadStrategy();
        return Optional.ofNullable(toSell.get());
    }

    public Optional<PurchaseStrategy> getToPurchase() {
        possiblyReloadStrategy();
        return Optional.ofNullable(toPurchase.get());
    }

    public Optional<ReservationStrategy> getForReservations() {
        possiblyReloadStrategy();
        return Optional.ofNullable(forReservations.get());
    }
}
