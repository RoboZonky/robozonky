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

package com.github.triceo.robozonky.api.confirmations;

import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class ConfirmationTest {

    @Test
    public void amountLessConstructor() {
        final Confirmation c = new Confirmation(ConfirmationType.REJECTED);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(c.getAmount()).isEmpty();
        softly.assertThat(c.getType()).isSameAs(ConfirmationType.REJECTED);
        softly.assertAll();
    }

    @Test
    public void constructorWithAmount() {
        final Confirmation c = new Confirmation(200);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(c.getAmount()).isEqualTo(200);
        softly.assertThat(c.getType()).isSameAs(ConfirmationType.APPROVED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithWrongAmount() {
        new Confirmation(Defaults.MINIMUM_INVESTMENT_IN_CZK - 1);
    }

    @Test
    public void equalsWithItself() {
        final int amount = Defaults.MINIMUM_INVESTMENT_IN_CZK;
        final Confirmation c1 = new Confirmation(amount);
        Assertions.assertThat(c1).isEqualTo(c1);
        final Confirmation c2 = new Confirmation(amount);
        Assertions.assertThat(c1).isEqualTo(c2);
    }

    @Test
    public void notEqualsWithDifferentAmount() {
        final Confirmation c1 = new Confirmation(200);
        final Confirmation c2 = new Confirmation(300);
        Assertions.assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    public void notEqualsWithDifferentConfirmationType() {
        final Confirmation c1 = new Confirmation(ConfirmationType.REJECTED);
        final Confirmation c2 = new Confirmation(300);
        Assertions.assertThat(c1).isNotEqualTo(c2);
        final Confirmation c3 = new Confirmation(ConfirmationType.DELEGATED);
        Assertions.assertThat(c1).isNotEqualTo(c3);
    }

    @Test
    public void notEqualsWithDifferentJavaType() {
        final Confirmation c1 = new Confirmation(ConfirmationType.REJECTED);
        Assertions.assertThat(c1).isNotEqualTo(c1.toString());
    }
}
