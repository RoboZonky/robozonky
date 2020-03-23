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

package com.github.robozonky.strategy.natural;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.api.strategies.StrategyService;
import com.github.robozonky.internal.Defaults;

public class NaturalLanguageStrategyService implements StrategyService {

    private static final Logger LOGGER = LogManager.getLogger(NaturalLanguageStrategyService.class);
    private static final AtomicReference<Map<String, ParsedStrategy>> CACHE = new AtomicReference<>(
            Collections.emptyMap());
    private static final ANTLRErrorListener ERROR_LISTENER = new BaseErrorListener() {

        @Override
        public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                final int charPositionInLine, final String msg, final RecognitionException e) {
            final String error = "Syntax error at " + line + ":" + charPositionInLine + ", offending symbol "
                    + offendingSymbol + ", message: " + msg;
            throw new IllegalStateException(error, e);
        }

    };

    private static void setCached(final String strategy, final ParsedStrategy parsed) {
        CACHE.set(Collections.singletonMap(strategy, parsed));
        LOGGER.debug("Cached strategy: {}.", parsed);
    }

    private static Optional<ParsedStrategy> getCached(final String strategy) {
        return Optional.ofNullable(CACHE.get()
            .get(strategy));
    }

    private static synchronized ParsedStrategy parseOrCached(final String strategy) {
        return getCached(strategy).orElseGet(() -> {
            LOGGER.trace("Parsing started.");
            final ParsedStrategy parsed = parseWithAntlr(CharStreams.fromString(strategy));
            LOGGER.trace("Parsing finished.");
            setCached(strategy, parsed);
            return parseOrCached(strategy); // call itself again, making use of the cache
        });
    }

    static ParsedStrategy parseWithAntlr(final CharStream s) {
        final NaturalLanguageStrategyLexer l = new NaturalLanguageStrategyLexer(s);
        l.removeErrorListeners(); // no sysout
        final CommonTokenStream ts = new CommonTokenStream(l);
        final NaturalLanguageStrategyParser p = new NaturalLanguageStrategyParser(ts);
        p.removeErrorListeners(); // turn sysout into an exception
        p.addErrorListener(ERROR_LISTENER);
        return p.primaryExpression().result;
    }

    private static boolean isSupported(final ParsedStrategy s) {
        final String currentVersion = Defaults.ROBOZONKY_VERSION;
        return s.getMinimumVersion()
            .map(minimum -> new RoboZonkyVersion(currentVersion).compareTo(minimum) >= 0)
            .orElse(true);
    }

    private static <T> Optional<T> getStrategy(final String strategy, final Function<ParsedStrategy, T> constructor) {
        try {
            final ParsedStrategy s = parseOrCached(strategy);
            if (isSupported(s)) {
                return Optional.ofNullable(constructor.apply(s));
            }
            LOGGER.warn("Strategy does not support your version of RoboZonky. Please upgrade.");
        } catch (final Exception ex) {
            LOGGER.trace("Failed parsing strategy.", ex);
            LOGGER.debug("Failed parsing strategy, may still try using other formats. Reason: {}.", ex.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<InvestmentStrategy> toInvest(final String strategy) {
        return getStrategy(strategy, s -> s.isInvestingEnabled() ? new NaturalLanguageInvestmentStrategy(s) : null);
    }

    @Override
    public Optional<SellStrategy> toSell(final String strategy) {
        return getStrategy(strategy, s -> s.getSellingMode()
            .map(m -> new NaturalLanguageSellStrategy(s))
            .orElse(null));
    }

    @Override
    public Optional<PurchaseStrategy> toPurchase(final String strategy) {
        return getStrategy(strategy, s -> s.isPurchasingEnabled() ? new NaturalLanguagePurchaseStrategy(s) : null);
    }

    @Override
    public Optional<ReservationStrategy> forReservations(final String strategy) {
        return getStrategy(strategy, s -> s.getReservationMode()
            .isPresent() ? new NaturalLanguageReservationStrategy(s) : null);
    }
}
