/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.extensions;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.api.strategies.StrategyService;
import com.github.robozonky.internal.util.StreamUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements Java's {@link ServiceLoader} to provide suitable strategy implementations.
 */
public final class StrategyLoader {

    private static final Logger LOGGER = LogManager.getLogger(StrategyLoader.class);
    private static final Supplier<ServiceLoader<StrategyService>> LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(StrategyService.class);

    private StrategyLoader() {
        // no instances
    }

    static <T> Optional<T> processStrategyService(final StrategyService service, final String strategy,
                                                  final BiFunction<StrategyService, String, Optional<T>> getter) {
        try {
            return getter.apply(service, strategy);
        } catch (final Exception ex) {
            LOGGER.error("Failed reading strategy.", ex);
            return Optional.empty();
        }
    }

    static <T> Optional<T> load(final String strategy, final Iterable<StrategyService> loader,
                                final BiFunction<StrategyService, String, Optional<T>> provider) {
        return StreamUtil.toStream(loader)
                .map(iss -> processStrategyService(iss, strategy, provider))
                .flatMap(Optional::stream)
                .findFirst();
    }

    public static Optional<InvestmentStrategy> toInvest(final String strategy) {
        LOGGER.debug("Reading investment strategy.");
        return load(strategy, LOADER.get(), StrategyService::toInvest);
    }

    public static Optional<SellStrategy> toSell(final String strategy) {
        LOGGER.debug("Reading selling strategy.");
        return load(strategy, LOADER.get(), StrategyService::toSell);
    }

    public static Optional<PurchaseStrategy> toPurchase(final String strategy) {
        LOGGER.debug("Reading purchasing strategy.");
        return load(strategy, LOADER.get(), StrategyService::toPurchase);
    }

    public static Optional<ReservationStrategy> forReservations(final String strategy) {
        LOGGER.debug("Reading strategy for reservations.");
        return load(strategy, LOADER.get(), StrategyService::forReservations);
    }
}

