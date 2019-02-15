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

package com.github.robozonky.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.LongAdder;

import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class StrategyValidationFeatureTest {

    private static final String STRATEGY_MISSING_SELL = "- Obecná nastavení\n" +
            "Robot má udržovat konzervativní portfolio.\n" +
            "Robot má pravidelně kontrolovat rezervační systém " +
            "a přijímat rezervace půjček odpovídajících této strategii.\n" +
            "Běžná výše investice je 200 Kč.\n" +
            "Investovat do všech půjček a participací.\n" +
            "Prodej participací zakázán.";
    private static final String STRATEGY_MISSING_EVERYTHING = "- Obecná nastavení\n" +
            "Robot má udržovat konzervativní portfolio.\n" +
            "Robot má zcela ignorovat rezervační systém.\n" +
            "Běžná výše investice je 200 Kč.\n" +
            "Ignorovat všechny půjčky i participace.\n" +
            "Prodej participací zakázán.";
    private static final String STRATEGY_WITH_EVERYTHING = "- Obecná nastavení\n" +
            "Robot má udržovat konzervativní portfolio.\n" +
            "Robot má převzít kontrolu nad rezervačním systémem " +
            "a přijímat rezervace půjček odpovídajících této strategii.\n" +
            "Běžná výše investice je 200 Kč.\n" +
            "\n" +
            "- Filtrování tržiště\n" +
            "Investovat do všech půjček.\n" +
            "Investovat do všech participací.\n" +
            "\n" +
            "- Prodej participací\n" +
            "Prodat participaci, kde: úrok nedosahuje 5,0 % p.a.";

    @Test
    void failsOnNonExistentFile() throws IOException {
        final File f = File.createTempFile("robozonky-", ".strategy");
        f.delete();
        final Feature feature = new StrategyValidationFeature(f);
        assertThatThrownBy(feature::setup).isInstanceOf(SetupFailedException.class);
    }

    @Test
    void failsOnEmptyStrategy() throws IOException, SetupFailedException {
        final File f = File.createTempFile("robozonky-", ".strategy");
        Files.write(f.toPath(), "".getBytes(Defaults.CHARSET));
        final Feature feature = new StrategyValidationFeature(f);
        feature.setup();
        assertThatThrownBy(feature::test).isInstanceOf(TestFailedException.class);
    }

    @Test
    void failsOnNoStrategy() throws IOException, SetupFailedException {
        final File f = File.createTempFile("robozonky-", ".strategy");
        Files.write(f.toPath(), STRATEGY_MISSING_EVERYTHING.getBytes(Defaults.CHARSET));
        final Feature feature = new StrategyValidationFeature(f);
        feature.setup();
        assertThatThrownBy(feature::test).isInstanceOf(TestFailedException.class);
    }

    @Test
    void passesOnMissingSellStrategy() throws IOException, SetupFailedException, TestFailedException {
        final File f = File.createTempFile("robozonky-", ".strategy");
        Files.write(f.toPath(), STRATEGY_MISSING_SELL.getBytes(Defaults.CHARSET));
        final LongAdder adder = new LongAdder();
        final Feature feature = new StrategyValidationFeature(f, adder);
        feature.setup();
        feature.test();
        assertThat(adder.sum()).as("Strategy count with sell missing.").isEqualTo(3);
    }

    @Test
    void repeatValidationPasses() throws IOException, SetupFailedException, TestFailedException {
        final File f = File.createTempFile("robozonky-", ".strategy");
        Files.write(f.toPath(), STRATEGY_MISSING_SELL.getBytes(Defaults.CHARSET));
        final LongAdder adder = new LongAdder();
        final Feature feature = new StrategyValidationFeature(f, adder);
        feature.setup();
        feature.test();
        feature.test();
        assertThat(adder.sum()).as("Strategy count with sell missing.").isEqualTo(3);
    }

    @Test
    void passesOnCompleteSellStrategy() throws IOException, SetupFailedException, TestFailedException {
        final File f = File.createTempFile("robozonky-", ".strategy");
        Files.write(f.toPath(), STRATEGY_WITH_EVERYTHING.getBytes(Defaults.CHARSET));
        final LongAdder adder = new LongAdder();
        final Feature feature = new StrategyValidationFeature(f, adder);
        feature.setup();
        feature.test();
        assertThat(adder.sum()).as("Strategy count has everything.").isEqualTo(4);
    }
}
