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

package com.github.robozonky.app.daemon;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.github.robozonky.internal.api.Defaults;
import com.google.common.io.Files;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class StrategyProviderTest {

    private static final String MINIMAL_STRATEGY = "Robot má udržovat konzervativní portfolio.";

    @Test
    void setAndWrong() {
        final StrategyProvider r = new StrategyProvider();
        r.valueSet(MINIMAL_STRATEGY); // store correct strategy
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isPresent();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isPresent();
        });
        r.valueChanged(MINIMAL_STRATEGY, UUID.randomUUID().toString()); // store invalid strategy
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isEmpty();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isEmpty();
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
        });
        r.valueUnset(MINIMAL_STRATEGY);
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isEmpty();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isEmpty();
        });
    }

    private static File newStrategyFile() throws IOException {
        final File strategy = File.createTempFile("robozonky-strategy", ".cfg");
        Files.write(MINIMAL_STRATEGY, strategy, Defaults.CHARSET);
        return strategy;
    }

    @Test
    void loadStrategyAsFile() throws IOException {
        final StrategyProvider r = StrategyProvider.createFor(newStrategyFile().getAbsolutePath());
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isPresent();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isPresent();
        });
    }

    @Test
    void loadWrongStrategyAsFile() throws IOException {
        final File tmp = File.createTempFile("robozonky-", ".cfg");
        final StrategyProvider r = StrategyProvider.createFor(tmp.getAbsolutePath());
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isEmpty();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isEmpty();
        });
    }

    @Test
    void loadStrategyAsUrl() throws IOException {
        final String url = newStrategyFile().toURI().toURL().toString();
        final StrategyProvider r = StrategyProvider.createFor(url);
        assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isPresent();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isPresent();
        });
    }

}
