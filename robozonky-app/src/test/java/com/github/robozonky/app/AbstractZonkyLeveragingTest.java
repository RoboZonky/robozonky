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

package com.github.robozonky.app;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Random;

import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.LoanBuilder;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.daemon.BlockedAmountProcessor;
import com.github.robozonky.app.daemon.LoanCache;
import com.github.robozonky.app.daemon.Portfolio;
import com.github.robozonky.app.daemon.RemoteBalance;
import com.github.robozonky.app.daemon.Transactional;
import com.github.robozonky.app.daemon.TransactionalPortfolio;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Settings;
import org.junit.jupiter.api.AfterEach;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractZonkyLeveragingTest extends AbstractEventLeveragingTest {

    private static final Random RANDOM = new Random(0);

    protected static RemoteBalance mockBalance(final Zonky zonky) {
        return new MockedBalance(zonky);
    }

    protected static MyInvestment mockMyInvestment() {
        return mockMyInvestment(OffsetDateTime.now());
    }

    private static MyInvestment mockMyInvestment(final OffsetDateTime creationDate) {
        final MyInvestment m = mock(MyInvestment.class);
        when(m.getId()).thenReturn(RANDOM.nextLong());
        when(m.getTimeCreated()).thenReturn(creationDate);
        return m;
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

    protected static TransactionalPortfolio createTransactionalPortfolio() {
        final Zonky zonky = harmlessZonky(10_000);
        return createTransactionalPortfolio(zonky);
    }

    protected static TransactionalPortfolio createTransactionalPortfolio(final Zonky zonky) {
        final Tenant tenant = mockTenant(zonky);
        final Portfolio portfolio = Portfolio.create(tenant, BlockedAmountProcessor.createLazy(tenant));
        return new TransactionalPortfolio(portfolio, tenant);
    }

    protected static Transactional createTransactional() {
        final Zonky zonky = harmlessZonky(10_000);
        return createTransactional(zonky);
    }

    protected static Transactional createTransactional(final Zonky zonky) {
        final Tenant tenant = mockTenant(zonky);
        return new Transactional(tenant);
    }

    private static LoanDescriptor mockLoanDescriptor(final int loanId, final boolean withCaptcha) {
        final LoanBuilder b = Loan.custom()
                .setId(loanId)
                .setRemainingInvestment(Integer.MAX_VALUE)
                .setDatePublished(OffsetDateTime.now());
        if (withCaptcha) {
            System.setProperty(Settings.Key.CAPTCHA_DELAY_D.getName(), "120"); // enable CAPTCHA for the rating
            b.setRating(Rating.D);
        } else {
            System.setProperty(Settings.Key.CAPTCHA_DELAY_AAAAA.getName(), "0"); // disable CAPTCHA for the rating
            b.setRating(Rating.AAAAA);
        }
        return new LoanDescriptor(b.build());
    }

    @AfterEach
    public void clearCache() {
        LoanCache.get().clean();
    }

    private static final class MockedBalance implements RemoteBalance {

        private final Zonky zonky;
        private BigDecimal difference = BigDecimal.ZERO;

        public MockedBalance(final Zonky zonky) {
            this.zonky = zonky;
        }

        @Override
        public void update(final BigDecimal change) {
            difference = difference.add(change);
        }

        @Override
        public BigDecimal get() {
            return zonky.getWallet().getAvailableBalance().add(difference);
        }

        @Override
        public void close() {

        }
    }
}
