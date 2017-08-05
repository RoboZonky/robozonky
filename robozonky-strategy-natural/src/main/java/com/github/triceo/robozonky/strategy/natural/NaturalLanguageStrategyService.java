/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.natural;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.PurchaseStrategy;
import com.github.triceo.robozonky.api.strategies.SellStrategy;
import com.github.triceo.robozonky.api.strategies.StrategyService;
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

    private static ParsedStrategy parseWithAntlr(final InputStream strategy) throws IOException {
        final CharStream s = CharStreams.fromStream(strategy);
        final NaturalLanguageStrategyLexer l = new NaturalLanguageStrategyLexer(s);
        final NaturalLanguageStrategyParser p = new NaturalLanguageStrategyParser(new CommonTokenStream(l));
        p.addErrorListener(NaturalLanguageStrategyService.ERROR_LISTENER);
        return p.primaryExpression().result;
    }

    @Override
    public Optional<InvestmentStrategy> toInvest(final InputStream strategy) {
        try {
            final ParsedStrategy s = NaturalLanguageStrategyService.parseWithAntlr(strategy);
            return Optional.of(new NaturalLanguageInvestmentStrategy(s));
        } catch (final Exception ex) {
            NaturalLanguageStrategyService.LOGGER.debug("Failed parsing strategy.", ex);
            return Optional.empty();
        }
    }

    @Override
    public Optional<SellStrategy> toSell(final InputStream strategy) {
        return Optional.empty(); // FIXME implement
    }

    @Override
    public Optional<PurchaseStrategy> toPurchase(final InputStream strategy) {
        return Optional.empty(); // FIXME implement
    }
}
