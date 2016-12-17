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

package com.github.triceo.robozonky;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.github.triceo.robozonky.api.events.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.events.InvestmentRequestedEvent;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class CoreInvestorTest extends AbstractInvestingTest {

    @Test
    public void singleInvestment() {
        // prepare API
        final int loanId = 1;
        final int loanAmount = 1000;
        final int originalBalance = 10000;
        final Loan loan = AbstractInvestingTest.mockLoan(loanId);
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getLoan(ArgumentMatchers.eq(loanId))).thenReturn(loan);
        // make sure no actual investing is being done
        final ZonkyProxy proxy = new ZonkyProxy.Builder().asDryRun().build(api);
        final Investor i = new Investor(proxy, BigDecimal.valueOf(originalBalance));
        // execute and test
        final Optional<Investment> result = i.invest(loanId, loanAmount, Duration.ofSeconds(0));
        Assertions.assertThat(result).isPresent();
        final Investment investment = result.get();
        Assertions.assertThat(investment.getLoanId()).isEqualTo(loanId);
        Assertions.assertThat(investment.getAmount()).isEqualTo(loanAmount);
        Assertions.assertThat(i.getBalance()).isEqualTo(BigDecimal.valueOf(originalBalance - loanAmount));
        Mockito.verify(this.getListener(), Mockito.times(1))
                .handle(ArgumentMatchers.any(InvestmentRequestedEvent.class));
        Mockito.verify(this.getListener(), Mockito.times(1))
                .handle(ArgumentMatchers.any(InvestmentMadeEvent.class));
    }

    @Test
    public void emptyInvestmentLoop() {
        // prepare API
        final int originalBalance = 10000;
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        // make sure no actual investing is being done
        final ZonkyProxy proxy = new ZonkyProxy.Builder().asDryRun().build(api);
        final Investor i = new Investor(proxy, BigDecimal.valueOf(originalBalance));
        final Collection<Investment> result = i.invest(Mockito.mock(InvestmentStrategy.class), Collections.emptyList());
        Assertions.assertThat(result).isEmpty();
    }

}
