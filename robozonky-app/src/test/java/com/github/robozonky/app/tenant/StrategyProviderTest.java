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

package com.github.robozonky.app.tenant;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.test.AbstractRoboZonkyTest;

class StrategyProviderTest extends AbstractRoboZonkyTest {

    private static final String MINIMAL_STRATEGY = "Tato strategie vyžaduje RoboZonky ve verzi 5.7.0 nebo pozdější.\n" +
            "- Obecná nastavení\n" +
            "Robot má udržovat konzervativní portfolio.\n" +
            "Robot má pravidelně kontrolovat rezervační systém a přijímat rezervace půjček odpovídajících této " +
            "strategii.\n" +
            "Robot má investovat do půjček po 200 Kč.\n" +
            "Robot má nakupovat participace nejvýše za 200 Kč.\n" +
            "Investovat do všech půjček a participací.\n" +
            "Prodávat všechny participace bez poplatku a slevy, které odpovídají filtrům tržiště.";

    private static File newStrategyFile() throws IOException {
        final File strategy = File.createTempFile("robozonky-strategy", ".cfg");
        Files.write(strategy.toPath(), MINIMAL_STRATEGY.getBytes(Defaults.CHARSET));
        return strategy;
    }

    @Test
    void setAndWrong() {
        final StrategyProvider r = new StrategyProvider();
        r.newValue(MINIMAL_STRATEGY); // store correct strategy
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest())
                .isPresent();
            softly.assertThat(r.getToSell())
                .isPresent();
            softly.assertThat(r.getToPurchase())
                .isPresent();
            softly.assertThat(r.getForReservations())
                .isPresent();
        });
        r.newValue(UUID.randomUUID()
            .toString()); // store invalid strategy
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest())
                .isEmpty();
            softly.assertThat(r.getToSell())
                .isEmpty();
            softly.assertThat(r.getToPurchase())
                .isEmpty();
            softly.assertThat(r.getForReservations())
                .isEmpty();
        });
    }

    @Test
    void setAndUnset() {
        final StrategyProvider r = new StrategyProvider();
        r.newValue(MINIMAL_STRATEGY); // store correct strategy
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest())
                .isPresent();
            softly.assertThat(r.getToSell())
                .isPresent();
            softly.assertThat(r.getToPurchase())
                .isPresent();
            softly.assertThat(r.getForReservations())
                .isPresent();
        });
        r.valueUnset();
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest())
                .isEmpty();
            softly.assertThat(r.getToSell())
                .isEmpty();
            softly.assertThat(r.getToPurchase())
                .isEmpty();
            softly.assertThat(r.getForReservations())
                .isEmpty();
        });
    }

    @Test
    void loadStrategyAsFile() throws IOException {
        final StrategyProvider r = StrategyProvider.createFor(newStrategyFile().getAbsolutePath());
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest())
                .isPresent();
            softly.assertThat(r.getToSell())
                .isPresent();
            softly.assertThat(r.getToPurchase())
                .isPresent();
            softly.assertThat(r.getForReservations())
                .isPresent();
        });
    }

    @Test
    void loadWrongStrategyAsFile() throws IOException {
        final File tmp = File.createTempFile("robozonky-", ".cfg");
        final StrategyProvider r = StrategyProvider.createFor(tmp.getAbsolutePath());
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest())
                .isEmpty();
            softly.assertThat(r.getToSell())
                .isEmpty();
            softly.assertThat(r.getToPurchase())
                .isEmpty();
            softly.assertThat(r.getForReservations())
                .isEmpty();
        });
    }

    @Test
    void loadWrongStrategyAsNonExistentFile() throws IOException {
        final File tmp = File.createTempFile("robozonky-", ".cfg");
        tmp.delete();
        final StrategyProvider r = StrategyProvider.createFor(tmp.getAbsolutePath());
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest())
                .isEmpty();
            softly.assertThat(r.getToSell())
                .isEmpty();
            softly.assertThat(r.getToPurchase())
                .isEmpty();
            softly.assertThat(r.getForReservations())
                .isEmpty();
        });
    }

    @Test
    void loadStrategyAsUrl() throws IOException {
        final String url = newStrategyFile().toURI()
            .toURL()
            .toString();
        final StrategyProvider r = StrategyProvider.createFor(url);
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest())
                .isPresent();
            softly.assertThat(r.getToSell())
                .isPresent();
            softly.assertThat(r.getToPurchase())
                .isPresent();
            softly.assertThat(r.getForReservations())
                .isPresent();
        });
    }

    @Test
    void empty() {
        final StrategyProvider r = StrategyProvider.empty();
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest())
                .isEmpty();
            softly.assertThat(r.getToPurchase())
                .isEmpty();
            softly.assertThat(r.getToSell())
                .isEmpty();
            softly.assertThat(r.getForReservations())
                .isEmpty();
        });
    }
}
