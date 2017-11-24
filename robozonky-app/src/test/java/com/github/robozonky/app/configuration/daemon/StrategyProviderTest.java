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

package com.github.robozonky.app.configuration.daemon;

import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class StrategyProviderTest {

    private static final String MINIMAL_STRATEGY = "Robot má udržovat konzervativní portfolio.";

    @Test
    public void setAndWrong() {
        final StrategyProvider r = new StrategyProvider();
        r.valueSet(MINIMAL_STRATEGY); // store correct strategy
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isPresent();
            softly.assertThat(r.getToSell()).isPresent();
            softly.assertThat(r.getToPurchase()).isPresent();
        });
        r.valueChanged(MINIMAL_STRATEGY, UUID.randomUUID().toString()); // store invalid strategy
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isEmpty();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isEmpty();
        });
    }

    @Test
    public void setAndUnset() {
        final StrategyProvider r = new StrategyProvider();
        r.valueSet(MINIMAL_STRATEGY); // store correct strategy
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isPresent();
            softly.assertThat(r.getToSell()).isPresent();
            softly.assertThat(r.getToPurchase()).isPresent();
        });
        r.valueUnset(MINIMAL_STRATEGY);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isEmpty();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isEmpty();
        });
    }
}
