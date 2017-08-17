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

package com.github.triceo.robozonky.api.remote.enums;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class PaymentStatusesTest {

    @Test
    public void ofArrayOfPaymentStatuses() {
        final PaymentStatus[] statuses = new PaymentStatus[]{PaymentStatus.OK, PaymentStatus.COVERED};
        final PaymentStatuses result = PaymentStatuses.of(statuses);
        Assertions.assertThat(result.getPaymentStatuses()).containsOnly(statuses);
    }

    @Test
    public void ofAllPaymentStatuses() {
        final PaymentStatuses result = PaymentStatuses.all();
        Assertions.assertThat(result.getPaymentStatuses()).containsOnly(PaymentStatus.values());
    }

    @Test
    public void correctToString() {
        // test no items
        Assertions.assertThat(PaymentStatuses.of(new PaymentStatus[]{}).toString()).isEqualTo("[]");
        // test one item
        Assertions.assertThat(PaymentStatuses.of(PaymentStatus.OK).toString()).isEqualTo("[OK]");
        // test multiple items
        Assertions.assertThat(PaymentStatuses.of(PaymentStatus.OK, PaymentStatus.COVERED).toString())
                .isEqualTo("[OK, COVERED]");
    }

    @Test
    public void correctValueOf() {
        // test no items
        Assertions.assertThat(PaymentStatuses.valueOf("[]").getPaymentStatuses()).isEmpty();
        Assertions.assertThat(PaymentStatuses.valueOf(" [ ] ").getPaymentStatuses()).isEmpty();
        // test one item
        Assertions.assertThat(PaymentStatuses.valueOf("[ COVERED ]").getPaymentStatuses())
                .containsExactly(PaymentStatus.COVERED);
        Assertions.assertThat(PaymentStatuses.valueOf(" [ COVERED]").getPaymentStatuses())
                .containsExactly(PaymentStatus.COVERED);
        // test multiple items
        Assertions.assertThat(PaymentStatuses.valueOf(" [COVERED, OK]").getPaymentStatuses())
                .containsExactly(PaymentStatus.OK, PaymentStatus.COVERED);
        Assertions.assertThat(PaymentStatuses.valueOf(" [OK, COVERED]").getPaymentStatuses())
                .containsExactly(PaymentStatus.OK, PaymentStatus.COVERED);
    }

    @Test
    public void invalidValueOf() {
        Assertions.assertThatThrownBy(() -> PaymentStatuses.valueOf("["))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void unknownValueOf() {
        Assertions.assertThatThrownBy(() -> PaymentStatuses.valueOf("[SOME_UNKNOWN_VALUE]"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
