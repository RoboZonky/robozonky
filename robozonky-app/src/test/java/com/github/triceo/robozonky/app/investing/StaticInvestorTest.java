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

package com.github.triceo.robozonky.app.investing;

import com.github.triceo.robozonky.api.confirmations.Confirmation;
import com.github.triceo.robozonky.api.confirmations.ConfirmationType;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class StaticInvestorTest extends AbstractInvestingTest {

    @Test
    public void investmentFromNullConfirmation() {
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor();
        final int recommended = 200;
        final Investment i = Investor.convertToInvestment(ld.recommend(recommended).get(), null);
        Assertions.assertThat(i.getAmount()).isEqualTo(recommended);
    }

    @Test
    public void investmentFromAmountlessConfirmation() {
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor();
        final int recommended = 200;
        final Investment i = Investor.convertToInvestment(ld.recommend(recommended).get(),
                                                          new Confirmation(ConfirmationType.DELEGATED));
        Assertions.assertThat(i.getAmount()).isEqualTo(recommended);
    }
}
