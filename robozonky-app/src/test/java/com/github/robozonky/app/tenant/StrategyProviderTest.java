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

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.google.common.io.Files;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class StrategyProviderTest extends AbstractRoboZonkyTest {

    private static final String MINIMAL_STRATEGY = "Robot má udržovat konzervativní portfolio.";

    private static File newStrategyFile() throws IOException {
        final File strategy = File.createTempFile("robozonky-strategy", ".cfg");
        Files.write(MINIMAL_STRATEGY, strategy, Defaults.CHARSET);
        return strategy;
    }

    @Test
    void setAndWrong() {
        final StrategyProvider r = new StrategyProvider();
        r.valueSet(MINIMAL_STRATEGY); // store correct strategy
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isPresent();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isPresent();
            softly.assertThat(r.getForReservations()).isEmpty();
        });
        r.valueChanged(MINIMAL_STRATEGY, UUID.randomUUID().toString()); // store invalid strategy
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isEmpty();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isEmpty();
            softly.assertThat(r.getForReservations()).isEmpty();
        });
    }

    @Test
    void setAndUnset() {
        final StrategyProvider r = new StrategyProvider();
        r.valueSet(MINIMAL_STRATEGY); // store correct strategy
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isPresent();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isPresent();
            softly.assertThat(r.getForReservations()).isEmpty();
        });
        r.valueUnset(MINIMAL_STRATEGY);
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isEmpty();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isEmpty();
            softly.assertThat(r.getForReservations()).isEmpty();
        });
    }

    @Test
    void loadStrategyAsFile() throws IOException, ExecutionException, InterruptedException {
        final StrategyProvider r = StrategyProvider.createFor(newStrategyFile().getAbsolutePath()).get();
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isPresent();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isPresent();
            softly.assertThat(r.getForReservations()).isEmpty();
        });
    }

    @Test
    void loadWrongStrategyAsFile() throws IOException, ExecutionException, InterruptedException {
        final File tmp = File.createTempFile("robozonky-", ".cfg");
        final StrategyProvider r = StrategyProvider.createFor(tmp.getAbsolutePath()).get();
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isEmpty();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isEmpty();
            softly.assertThat(r.getForReservations()).isEmpty();
        });
    }

    @Test
    void loadStrategyAsUrl() throws IOException, ExecutionException, InterruptedException {
        final String url = newStrategyFile().toURI().toURL().toString();
        final StrategyProvider r = StrategyProvider.createFor(url).get();
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isPresent();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isPresent();
            softly.assertThat(r.getForReservations()).isEmpty();
        });
    }

    @Test
    void empty() {
        final StrategyProvider r = StrategyProvider.empty();
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isEmpty();
            softly.assertThat(r.getToPurchase()).isEmpty();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getForReservations()).isEmpty();
        });
    }
}
