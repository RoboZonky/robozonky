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

package com.github.robozonky.test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.common.async.Tasks;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.common.tenant.RemotePortfolio;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.util.AbstractMinimalRoboZonkyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * This is a suggested parent class for all RoboZonky tests using this module. It will make sure to clear shared state
 * before and after each state, so that tests don't have unexpected and well-hidden dependencies.
 */
public abstract class AbstractRoboZonkyTest extends AbstractMinimalRoboZonkyTest {

    protected static final SessionInfo SESSION = new SessionInfo("someone@robozonky.cz", "Testing",
                                                                 false),
            SESSION_DRY = new SessionInfo("someone@robozonky.cz", "Testing", true);

    protected static Zonky harmlessZonky(final int availableBalance) {
        final Zonky zonky = mock(Zonky.class);
        final BigDecimal balance = BigDecimal.valueOf(availableBalance);
        when(zonky.getWallet()).thenReturn(new Wallet(1, 2, balance, balance));
        when(zonky.getRestrictions()).thenReturn(new Restrictions(true));
        when(zonky.getBlockedAmounts()).thenAnswer(i -> Stream.empty());
        when(zonky.getStatistics()).thenReturn(Statistics.empty());
        when(zonky.getDevelopments(anyInt())).thenAnswer(i -> Stream.empty());
        return zonky;
    }

    public static RemotePortfolio mockPortfolio(final Zonky zonky) {
        final AtomicReference<BigDecimal> change = new AtomicReference<>(BigDecimal.ZERO);
        final RemotePortfolio p = mock(RemotePortfolio.class);
        doAnswer(i -> {
            final BigDecimal amount = i.getArgument(2);
            change.updateAndGet(old -> old.add(amount));
            return null;
        }).when(p).simulateCharge(anyInt(), any(), any());
        final Supplier<BigDecimal> balance = () -> zonky.getWallet().getBalance().subtract(change.get());
        when(p.getBalance()).thenAnswer(i -> balance.get());
        when(p.getOverview()).thenAnswer(i -> mockPortfolioOverview(balance.get().intValue()));
        return p;
    }

    protected static Tenant mockTenant(final Zonky zonky) {
        return mockTenant(zonky, true);
    }

    protected static Tenant mockTenant(final Zonky zonky, final boolean isDryRun) {
        final Tenant auth = new TestingTenant(isDryRun ? SESSION_DRY : SESSION, zonky);
        return spy(auth);
    }

    protected static Tenant mockTenant() {
        return mockTenant(harmlessZonky(10_000));
    }

    protected static ApiProvider mockApiProvider(final OAuth oauth, final Zonky z) {
        final ApiProvider api = mock(ApiProvider.class);
        when(api.oauth(any())).then(i -> {
            final Function<OAuth, ?> f = i.getArgument(0);
            return f.apply(oauth);
        });
        when(api.call(any(), any())).then(i -> {
            final Supplier<ZonkyApiToken> s = i.getArgument(1);
            s.get();
            final Function<Zonky, ?> f = i.getArgument(0);
            return f.apply(z);
        });
        doAnswer((Answer<Void>) invocation -> {
            final Consumer<Zonky> f = invocation.getArgument(0);
            f.accept(z);
            return null;
        }).when(api).run(any(), any());
        return api;
    }

    @BeforeAll
    static void loadSystemProperties() {
        SystemProperties.INSTANCE.save();
    }

    protected static PortfolioOverview mockPortfolioOverview(final int balance) {
        final PortfolioOverview po = mock(PortfolioOverview.class);
        when(po.getCzkAvailable()).thenReturn(BigDecimal.valueOf(balance));
        when(po.getCzkInvested()).thenReturn(BigDecimal.ZERO);
        when(po.getCzkInvested(any())).thenReturn(BigDecimal.ZERO);
        when(po.getCzkAtRisk()).thenReturn(BigDecimal.ZERO);
        when(po.getCzkAtRisk(any())).thenReturn(BigDecimal.ZERO);
        when(po.getShareAtRisk()).thenReturn(BigDecimal.ZERO);
        when(po.getShareOnInvestment(any())).thenReturn(BigDecimal.ZERO);
        when(po.getAtRiskShareOnInvestment(any())).thenReturn(BigDecimal.ZERO);
        when(po.getTimestamp()).thenReturn(ZonedDateTime.now());
        return po;
    }

    protected static PortfolioOverview mockPortfolioOverview() {
        return mockPortfolioOverview(0);
    }

    @AfterEach
    void restoreSystemProperties() {
        SystemProperties.INSTANCE.restore();
    }

    @AfterEach
    void closeSchedulers() {
        Tasks.closeAll();
    }

    @AfterEach
    protected void deleteState() {
        TenantState.destroyAll();
        logger.info("Destroyed state.");
    }
}
