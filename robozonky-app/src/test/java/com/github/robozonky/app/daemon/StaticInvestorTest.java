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

package com.github.robozonky.app.daemon;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class StaticInvestorTest extends AbstractZonkyLeveragingTest {

    @Test
    void investmentFromAmountlessConfirmation() {
        final LoanDescriptor ld = mockLoanDescriptor();
        final int recommended = 200;
        final Investment i = Investor.convertToInvestment(ld.recommend(recommended).get());
        assertThat(i.getOriginalPrincipal().intValue()).isEqualTo(recommended);
    }
}
