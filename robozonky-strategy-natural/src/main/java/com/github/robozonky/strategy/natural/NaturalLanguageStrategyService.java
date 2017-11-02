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

package com.github.robozonky.strategy.natural;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.api.strategies.StrategyService;
import com.github.robozonky.internal.api.Defaults;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NaturalLanguageStrategyService implements StrategyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NaturalLanguageStrategyService.class);
    private static final Map<String, ParsedStrategy> CACHE = new HashMap<>(1);

    private static final ANTLRErrorListener ERROR_LISTENER = new BaseErrorListener() {

        @Override
        public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                                final int charPositionInLine, final String msg, final RecognitionException e) {
            String sourceName = recognizer.getInputStream().getSourceName();
            if (!sourceName.isEmpty()) {
                sourceName = String.format("%s:%d:%d: ", sourceName, line, charPositionInLine);
            }
            final String error = sourceName + "line " + line + ":" + charPositionInLine + " " + msg
                    + ", offending symbol: " + offendingSymbol;
            throw new IllegalStateException("Syntax error: " + error, e);
        }
    };

    private synchronized static void setCached(final String strategy, final ParsedStrategy parsed) {
        CACHE.clear();
        CACHE.put(strategy, parsed);
        LOGGER.debug("Cached strategy: {}.", parsed);
    }

    private synchronized static Optional<ParsedStrategy> getCached(final String strategy) {
        if (CACHE.containsKey(strategy)) {
            return Optional.of(CACHE.get(strategy));
        } else {
            return Optional.empty();
        }
    }

    private synchronized static ParsedStrategy parseOrCached(final String strategy) throws IOException {
        final Optional<ParsedStrategy> cached = getCached(strategy);
        if (cached.isPresent()) {
            return cached.get();
        }
        try (final InputStream bis = new ByteArrayInputStream(strategy.getBytes(Defaults.CHARSET))) {
            setCached(strategy, parseWithAntlr(bis));
            return parseOrCached(strategy); // call itself again, making use of the cache
        }
    }

    static ParsedStrategy parseWithAntlr(final InputStream strategy) throws IOException {
        final CharStream s = CharStreams.fromStream(strategy);
        final NaturalLanguageStrategyLexer l = new NaturalLanguageStrategyLexer(s);
        l.removeErrorListeners(); // prevent any sysout
        final NaturalLanguageStrategyParser p = new NaturalLanguageStrategyParser(new CommonTokenStream(l));
        p.removeErrorListeners(); // prevent any sysout
        p.addErrorListener(NaturalLanguageStrategyService.ERROR_LISTENER);
        return p.primaryExpression().result;
    }

    @Override
    public Optional<InvestmentStrategy> toInvest(final String strategy) {
        try {
            final ParsedStrategy s = parseOrCached(strategy);
            return Optional.of(new NaturalLanguageInvestmentStrategy(s));
        } catch (final Exception ex) {
            NaturalLanguageStrategyService.LOGGER.debug("Failed parsing strategy, OK if using different: {}.",
                                                        ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<SellStrategy> toSell(final String strategy) {
        try {
            final ParsedStrategy s = parseOrCached(strategy);
            return Optional.of(new NaturalLanguageSellStrategy(s));
        } catch (final Exception ex) {
            NaturalLanguageStrategyService.LOGGER.debug("Failed parsing strategy, OK if using different: {}.",
                                                        ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<PurchaseStrategy> toPurchase(final String strategy) {
        try {
            final ParsedStrategy s = parseOrCached(strategy);
            return Optional.of(new NaturalLanguagePurchaseStrategy(s));
        } catch (final Exception ex) {
            NaturalLanguageStrategyService.LOGGER.debug("Failed parsing strategy, OK if using different: {}.",
                                                        ex.getMessage());
            return Optional.empty();
        }
    }
}
