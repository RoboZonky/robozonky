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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.remote.InvestmentFailureType;
import com.github.robozonky.internal.remote.InvestmentResult;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.mock.MockLoanBuilder;
import io.vavr.control.Either;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.vavr.api.VavrAssertions.assertThat;
import static org.mockito.Mockito.*;

class InvestorTest extends AbstractZonkyLeveragingTest {

    private static final Loan LOAN = new MockLoanBuilder()
            .setNonReservedRemainingInvestment(100_000)
            .build();
    private static final LoanDescriptor DESCRIPTOR = new LoanDescriptor(LOAN);

    private final Zonky zonky = harmlessZonky();

    @ParameterizedTest
    @ArgumentsSource(SessionType.class)
    void proper(final SessionInfo sessionType) {
        final Tenant t = mockTenant(zonky, sessionType.isDryRun());
        final Investor i = Investor.build(t);
        final RecommendedLoan r = DESCRIPTOR.recommend(Money.from(200)).orElse(null);
        final Either<InvestmentFailureType, Money> result = i.invest(r);
        assertThat(result).containsOnRight(Money.from(200));
    }

    @ParameterizedTest
    @ArgumentsSource(SessionType.class)
    void knownFail(final SessionInfo sessionType) {
        final boolean isDryRun = sessionType.isDryRun();
        final Tenant t = mockTenant(zonky, isDryRun);
        final Response failure = new ResponseBuilderImpl()
                .status(400)
                .entity(InvestmentFailureType.INSUFFICIENT_BALANCE.getReason().get())
                .build();
        when(zonky.invest(any())).thenReturn(InvestmentResult.failure(new BadRequestException(failure)));
        final Investor i = Investor.build(t);
        final RecommendedLoan r = DESCRIPTOR.recommend(Money.from(200)).orElse(null);
        final Either<InvestmentFailureType, Money> result = i.invest(r);
        if (isDryRun) { // the endpoint is not actually called, therefore cannot return error
            assertThat(result).containsOnRight(Money.from(200));
        } else {
            assertThat(result).containsOnLeft(InvestmentFailureType.INSUFFICIENT_BALANCE);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(SessionType.class)
    void unknownFail(final SessionInfo sessionType) {
        final boolean isDryRun = sessionType.isDryRun();
        final Tenant t = mockTenant(zonky, isDryRun);
        final Response failure = new ResponseBuilderImpl()
                .status(400)
                .build();
        when(zonky.invest(any())).thenReturn(InvestmentResult.failure(new BadRequestException(failure)));
        final Investor i = Investor.build(t);
        final RecommendedLoan r = DESCRIPTOR.recommend(Money.from(200)).orElse(null);
        final Either<InvestmentFailureType, Money> result = i.invest(r);
        if (isDryRun) { // the endpoint is not actually called, therefore cannot return error
            assertThat(result).containsOnRight(Money.from(200));
        } else {
            assertThat(result).containsOnLeft(InvestmentFailureType.UNKNOWN);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(SessionType.class)
    void irrelevantFail(final SessionInfo sessionType) {
        final boolean isDryRun = sessionType.isDryRun();
        final Tenant t = mockTenant(zonky, isDryRun);
        doThrow(IllegalStateException.class).when(zonky).invest(any());
        final Investor i = Investor.build(t);
        final RecommendedLoan r = DESCRIPTOR.recommend(Money.from(200)).orElse(null);
        if (isDryRun) { // the endpoint is not actually called, therefore cannot return error
            assertThat(i.invest(r)).containsOnRight(Money.from(200));
        } else {
            assertThatThrownBy(() -> i.invest(r)).isInstanceOf(IllegalStateException.class);
        }
    }

    private static final class SessionType implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
            return Stream.of(SESSION, SESSION_DRY).map(Arguments::arguments);
        }
    }
}
