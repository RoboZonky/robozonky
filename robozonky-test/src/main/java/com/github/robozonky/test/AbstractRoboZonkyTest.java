/*
 * Copyright 2020 The RoboZonky Project
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

import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.stubbing.Answer;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Consents;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.remote.InvestmentResult;
import com.github.robozonky.internal.remote.OAuth;
import com.github.robozonky.internal.remote.PurchaseResult;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.secrets.SecretProvider;
import com.github.robozonky.internal.state.TenantState;
import com.github.robozonky.internal.tenant.RemotePortfolio;
import com.github.robozonky.internal.tenant.Tenant;

/**
 * This is a suggested parent class for all RoboZonky tests using this module. It will make sure to clear shared state
 * before and after each state, so that tests don't have unexpected and well-hidden dependencies.
 */
public abstract class AbstractRoboZonkyTest extends AbstractMinimalRoboZonkyTest {

    private static final String USERNAME = "someone@robozonky.cz";
    protected static final SessionInfo SESSION = new SessionInfo(USERNAME, "Testing", false);
    protected static final SessionInfo SESSION_DRY = new SessionInfo(USERNAME, "Testing", true);

    protected static SecretProvider mockSecretProvider(final ZonkyApiToken token) {
        final SecretProvider s = SecretProvider.inMemory(USERNAME, "pwd".toCharArray());
        s.setToken(token);
        return s;
    }

    protected static SecretProvider mockSecretProvider() {
        return mockSecretProvider(new ZonkyApiToken(UUID.randomUUID()
            .toString(),
                UUID.randomUUID()
                    .toString(),
                299));
    }

    protected static Zonky harmlessZonky() {
        final Zonky zonky = mock(Zonky.class);
        when(zonky.invest(any())).thenReturn(InvestmentResult.success());
        when(zonky.purchase(any())).thenReturn(PurchaseResult.success());
        when(zonky.getRestrictions()).thenReturn(new Restrictions(true));
        when(zonky.getConsents()).thenReturn(new Consents());
        when(zonky.getStatistics()).thenReturn(Statistics.empty());
        when(zonky.getInvestments(any())).thenAnswer(i -> Stream.empty());
        return zonky;
    }

    public static RemotePortfolio mockPortfolio() {
        final AtomicReference<Money> change = new AtomicReference<>(Money.ZERO);
        final RemotePortfolio p = mock(RemotePortfolio.class);
        doAnswer(i -> {
            final Money amount = i.getArgument(2);
            change.updateAndGet(old -> old.add(amount));
            return null;
        }).when(p)
            .simulateCharge(anyInt(), any(), any());
        when(p.getOverview()).thenAnswer(i -> mockPortfolioOverview());
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
        return mockTenant(harmlessZonky());
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
        }).when(api)
            .run(any(), any());
        return api;
    }

    @BeforeAll
    static void loadSystemProperties() {
        SystemProperties.INSTANCE.save();
    }

    protected static ExtendedPortfolioOverview mockPortfolioOverview() {
        final ExtendedPortfolioOverview po = mock(ExtendedPortfolioOverview.class);
        when(po.getShareAtRisk()).thenCallRealMethod();
        when(po.getShareOnInvestment(any())).thenCallRealMethod();
        when(po.getAtRiskShareOnInvestment(any())).thenCallRealMethod();
        when(po.getShareSellable()).thenCallRealMethod();
        when(po.getShareSellable(any())).thenCallRealMethod();
        when(po.getShareSellableFeeless()).thenCallRealMethod();
        when(po.getShareSellableFeeless(any())).thenCallRealMethod();
        when(po.getMinimalMonthlyProfit()).thenCallRealMethod();
        when(po.getMonthlyProfit()).thenCallRealMethod();
        when(po.getOptimalMonthlyProfit()).thenCallRealMethod();
        when(po.getMinimalAnnualProfitability()).thenCallRealMethod();
        when(po.getOptimalAnnualProfitability()).thenCallRealMethod();
        when(po.getAnnualProfitability()).thenReturn(Ratio.fromPercentage(5));
        when(po.getInvested()).thenReturn(Money.ZERO);
        when(po.getInvested(any())).thenReturn(Money.ZERO);
        when(po.getAtRisk()).thenReturn(Money.ZERO);
        when(po.getAtRisk(any())).thenReturn(Money.ZERO);
        when(po.getSellable()).thenReturn(Money.ZERO);
        when(po.getSellable(any())).thenReturn(Money.ZERO);
        when(po.getSellableFeeless()).thenReturn(Money.ZERO);
        when(po.getSellableFeeless(any())).thenReturn(Money.ZERO);
        when(po.getTimestamp()).thenReturn(ZonedDateTime.now());
        return po;
    }

    @AfterEach
    void restoreSystemProperties() {
        SystemProperties.INSTANCE.restore();
    }

    @AfterEach
    protected void awaitTerminationOfParallelTasks() {
        logger.debug("Awaiting common ForkJoinPool quiescence.");
        final boolean success = ForkJoinPool.commonPool()
            .awaitQuiescence(1, TimeUnit.SECONDS);
        if (success) {
            logger.debug("All executors shut down.");
        } else {
            // our own cleanup may be failing, potentially leaving running threads behind
            throw new IllegalStateException("Common ForkJoinPool never quiescent, will likely kill all future tests.");
        }
    }

    @AfterEach
    protected void deleteState() {
        TenantState.destroyAll();
        logger.info("Destroyed state.");
    }
}
