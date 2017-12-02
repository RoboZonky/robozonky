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

package com.github.robozonky.app;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.internal.api.Settings;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AbstractZonkyLeveragingTest extends AbstractEventLeveragingTest {

    private static final Random RANDOM = new Random(0);

    protected static Loan mockLoan(final int loanId) {
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getId()).thenReturn(loanId);
        Mockito.when(loan.getRemainingInvestment()).thenReturn(Double.MAX_VALUE);
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        return loan;
    }

    protected static LoanDescriptor mockLoanDescriptor() {
        return AbstractZonkyLeveragingTest.mockLoanDescriptor(AbstractZonkyLeveragingTest.RANDOM.nextInt());
    }

    protected static LoanDescriptor mockLoanDescriptor(final int loanId) {
        return AbstractZonkyLeveragingTest.mockLoanDescriptor(loanId, true);
    }

    protected static LoanDescriptor mockLoanDescriptorWithoutCaptcha() {
        return AbstractZonkyLeveragingTest.mockLoanDescriptor(AbstractZonkyLeveragingTest.RANDOM.nextInt(), false);
    }

    protected static LoanDescriptor mockLoanDescriptor(final int loanId, final boolean withCaptcha) {
        final Loan loan = AbstractZonkyLeveragingTest.mockLoan(loanId);
        if (withCaptcha) {
            System.setProperty(Settings.Key.CAPTCHA_DELAY_D.getName(), "120"); // enable CAPTCHA for the rating
            Mockito.when(loan.getRating()).thenReturn(Rating.D);
        } else {
            System.setProperty(Settings.Key.CAPTCHA_DELAY_AAAAA.getName(), "0"); // disable CAPTCHA for the rating
            Mockito.when(loan.getRating()).thenReturn(Rating.AAAAA);
        }
        return new LoanDescriptor(loan);
    }

    protected static Zonky harmlessZonky(final int availableBalance) {
        final Zonky zonky = Mockito.mock(Zonky.class);
        final BigDecimal balance = BigDecimal.valueOf(availableBalance);
        Mockito.when(zonky.getWallet()).thenReturn(new Wallet(1, 2, balance, balance));
        Mockito.when(zonky.getBlockedAmounts()).thenReturn(Stream.empty());
        return zonky;
    }

    protected static Authenticated mockAuthentication(final Zonky zonky) {
        final Authenticated auth = Mockito.mock(Authenticated.class);
        Mockito.when(auth.getSecretProvider())
                .thenReturn(SecretProvider.fallback("someone", "password".toCharArray()));
        Mockito.doAnswer(invocation -> {
            final Function<Zonky, Object> operation = invocation.getArgument(0);
            return operation.apply(zonky);
        }).when(auth).call(ArgumentMatchers.any());
        Mockito.doAnswer(invocation -> {
            final Consumer<Zonky> operation = invocation.getArgument(0);
            operation.accept(zonky);
            return null;
        }).when(auth).run(ArgumentMatchers.any());
        return auth;
    }
}
