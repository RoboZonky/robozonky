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

package com.github.robozonky.strategy.natural;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

/**
 * ANTLR will, as a performance optimization, create several caches and store them in static fields. This will cause
 * permanent memory allocation. Since we only parse strategies every couple minutes and on a background thread,
 * parsing strategies using this class will sacrifice performance for long-term memory efficiency.
 * @see <a href="https://github.com/antlr/antlr4/issues/499">Github issue discussing the problem.</a>
 */
final class SideEffectFreeParser {

    private static final ANTLRErrorListener ERROR_LISTENER = new BaseErrorListener() {

        @Override
        public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                                final int charPositionInLine, final String msg, final RecognitionException e) {
            final String error = "Syntax error at " + line + ":" + charPositionInLine + ", offending symbol "
                    + offendingSymbol + ", message: " + msg;
            throw new IllegalStateException(error, e);
        }
    };

    /**
     * Will disable any writes to {@link System#out} for the given recognizer.
     * @param instance
     * @param <T>
     * @return Whatever was given as input.
     */
    private static <T extends Recognizer<?, ?>> T silence(final T instance) {
        instance.removeErrorListeners();
        return instance;
    }

    private static void modifyInterpreter(final NaturalLanguageStrategyParser p) {
        final int originalSize = p.getInterpreter().decisionToDFA.length;
        final DFA[] emptyDFA = new DFA[originalSize]; // give our own array so the static one isn't used
        final ParserATNSimulator newInterpreter =
                new ParserATNSimulator(p, p.getATN(), emptyDFA, new PredictionContextCache());
        newInterpreter.clearDFA(); // initialize our array so that the parser functions properly
        p.setInterpreter(newInterpreter); // replace the interpreter to bypass all static caches
    }

    private static void modifyInterpreter(final NaturalLanguageStrategyLexer l) {
        final int originalSize = l.getInterpreter().decisionToDFA.length;
        final DFA[] emptyDFA = new DFA[originalSize]; // give our own array so the static one isn't used
        final LexerATNSimulator newInterpreter =
                new LexerATNSimulator(l, l.getATN(), emptyDFA, new PredictionContextCache());
        newInterpreter.clearDFA(); // initialize our array so that the lexer functions properly
        l.setInterpreter(newInterpreter); // replace the interpreter to bypass all static caches
    }

    public static ParsedStrategy apply(final CharStream s) { // no refs from this method will be retained when it's over
        final NaturalLanguageStrategyLexer l = silence(new NaturalLanguageStrategyLexer(s));
        modifyInterpreter(l);
        final CommonTokenStream ts = new CommonTokenStream(l);
        final NaturalLanguageStrategyParser p = silence(new NaturalLanguageStrategyParser(ts));
        p.addErrorListener(ERROR_LISTENER);
        modifyInterpreter(p);
        return p.primaryExpression().result;
    }
}
