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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.LoanBuilder;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.portfolio.RemoteBalance;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public abstract class AbstractZonkyLeveragingTest extends AbstractEventLeveragingTest {

    private static final Random RANDOM = new Random(0);
    private static final SessionInfo SESSION = new SessionInfo("someone@robozonky.cz", "Testing",
                                                               false),
            SESSION_DRY = new SessionInfo("someone@robozonky.cz", "Testing", true);

    @SuppressWarnings("unchecked")
    protected static ApiProvider mockApiProvider(final OAuth oauth, final Zonky z) {
        final ApiProvider api = mock(ApiProvider.class);
        when(api.oauth(any())).then(i -> {
            final Function<OAuth, ?> f = i.getArgument(0);
            return f.apply(oauth);
        });
        when(api.call(any(Function.class), any())).then(i -> {
            final Supplier<ZonkyApiToken> s = i.getArgument(0);
            s.get();
            final Function<Zonky, ?> f = i.getArgument(1);
            return f.apply(z);
        });
        return api;
    }

    protected static RemoteBalance mockBalance(final Zonky zonky) {
        return new MockedBalance(zonky);
    }

    protected static MyInvestment mockMyInvestment() {
        return mockMyInvestment(OffsetDateTime.now());
    }

    private static MyInvestment mockMyInvestment(final OffsetDateTime creationDate) {
        final MyInvestment m = mock(MyInvestment.class);
        when(m.getId()).thenReturn(RANDOM.nextInt());
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

    protected static Zonky harmlessZonky(final int availableBalance) {
        final Zonky zonky = mock(Zonky.class);
        final BigDecimal balance = BigDecimal.valueOf(availableBalance);
        when(zonky.getWallet()).thenReturn(new Wallet(1, 2, balance, balance));
        when(zonky.getRestrictions()).thenReturn(new Restrictions());
        when(zonky.getBlockedAmounts()).thenAnswer(i -> Stream.empty());
        when(zonky.getStatistics()).thenReturn(Statistics.empty());
        when(zonky.getDevelopments(any())).thenAnswer(i -> Stream.empty());
        return zonky;
    }

    protected static Tenant mockTenant(final Zonky zonky) {
        return mockTenant(zonky, true);
    }

    protected static Tenant mockTenant(final Zonky zonky, final boolean isDryRun) {
        final Tenant auth = spy(Tenant.class);
        when(auth.getSessionInfo()).thenReturn(isDryRun ? SESSION_DRY : SESSION);
        when(auth.getRestrictions()).thenReturn(new Restrictions());
        doAnswer(invocation -> {
            final Function<Zonky, Object> operation = invocation.getArgument(0);
            return operation.apply(zonky);
        }).when(auth).call(any());
        doAnswer(invocation -> {
            final Consumer<Zonky> operation = invocation.getArgument(0);
            operation.accept(zonky);
            return null;
        }).when(auth).run(any());
        return auth;
    }

    protected static Tenant mockTenant() {
        return mockTenant(harmlessZonky(10_000));
    }

    @BeforeEach
    @AfterEach
    public void clearCache() {
        LoanCache.INSTANCE.clean();
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
    }
}
