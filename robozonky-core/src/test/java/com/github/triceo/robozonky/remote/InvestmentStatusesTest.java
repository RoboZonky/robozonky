/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.remote;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class InvestmentStatusesTest {

    @Test
    public void ofArrayOfInvestmentStatuses() {
        final InvestmentStatus[] statuses = new InvestmentStatus[] {InvestmentStatus.ACTIVE, InvestmentStatus.COVERED};
        final InvestmentStatuses result = InvestmentStatuses.of(statuses);
        Assertions.assertThat(result.getInvestmentStatuses()).containsOnly(statuses);
    }

    @Test
    public void ofAllInvestmentStatuses() {
        final InvestmentStatuses result = InvestmentStatuses.all();
        Assertions.assertThat(result.getInvestmentStatuses()).containsOnly(InvestmentStatus.values());
    }

    @Test
    public void correctToString() {
        // test no items
        Assertions.assertThat(InvestmentStatuses.of(new InvestmentStatus[] {}).toString()).isEqualTo("[]");
        // test one item
        Assertions.assertThat(InvestmentStatuses.of(InvestmentStatus.ACTIVE).toString()).isEqualTo("[ACTIVE]");
        // test multiple items
        Assertions.assertThat(InvestmentStatuses.of(InvestmentStatus.ACTIVE, InvestmentStatus.COVERED).toString())
                .isEqualTo("[COVERED, ACTIVE]");
    }

    @Test
    public void correctValueOf() {
        // test no items
        Assertions.assertThat(InvestmentStatuses.valueOf("[]").getInvestmentStatuses()).isEmpty();
        Assertions.assertThat(InvestmentStatuses.valueOf(" [ ] ").getInvestmentStatuses()).isEmpty();
        // test one item
        Assertions.assertThat(InvestmentStatuses.valueOf("[ COVERED ]").getInvestmentStatuses())
                .containsExactly(InvestmentStatus.COVERED);
        Assertions.assertThat(InvestmentStatuses.valueOf(" [ COVERED]").getInvestmentStatuses())
                .containsExactly(InvestmentStatus.COVERED);
        // test multiple items
        Assertions.assertThat(InvestmentStatuses.valueOf(" [COVERED, ACTIVE]").getInvestmentStatuses())
                .containsExactly(InvestmentStatus.COVERED, InvestmentStatus.ACTIVE);
        Assertions.assertThat(InvestmentStatuses.valueOf(" [ACTIVE, COVERED]").getInvestmentStatuses())
                .containsExactly(InvestmentStatus.COVERED, InvestmentStatus.ACTIVE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidValueOf() {
        InvestmentStatuses.valueOf("[");
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownValueOf() {
        InvestmentStatuses.valueOf("[SOME_UNKNOWN_VALUE]");
    }

}
