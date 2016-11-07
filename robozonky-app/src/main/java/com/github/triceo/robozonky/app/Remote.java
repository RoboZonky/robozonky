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
package com.github.triceo.robozonky.app;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import com.github.triceo.robozonky.ApiProvider;
import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.configuration.Configuration;
import com.github.triceo.robozonky.app.events.CaptchaProtectedLoanArrivalEvent;
import com.github.triceo.robozonky.app.events.ExecutionCompleteEvent;
import com.github.triceo.robozonky.app.events.ExecutionStartedEvent;
import com.github.triceo.robozonky.app.events.MarketplaceCheckCompleteEvent;
import com.github.triceo.robozonky.app.events.MarketplaceCheckStartedEvent;
import com.github.triceo.robozonky.app.events.UnprotectedLoanArrivalEvent;
import com.github.triceo.robozonky.events.EventRegistry;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.ZonkyApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base investing algorithm, ready to be executed with {@link ScheduledExecutorService}.
 */
public class Remote implements Callable<Optional<Collection<Investment>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Remote.class);
    static final Path MARKETPLACE_TIMESTAMP =
            Paths.get(System.getProperty("user.dir"), "robozonky.lastMarketplaceCheck.timestamp");

    static List<Loan> getAvailableLoans(final Configuration ctx, final Activity activity) {
        final boolean shouldSleep = activity.shouldSleep();
        if (shouldSleep) {
            Remote.LOGGER.info("RoboZonky is asleep as there is nothing going on.");
            return Collections.emptyList();
        } else {
            Remote.LOGGER.info("Ignoring loans published earlier than {} seconds ago.", ctx.getCaptchaDelayInSeconds());
            return Collections.unmodifiableList(activity.getAvailableLoans());
        }
    }

    static BigDecimal getAvailableBalance(final Configuration ctx, final ZonkyApi api) {
        return (ctx.isDryRun() && ctx.getDryRunBalance().isPresent()) ?
                BigDecimal.valueOf(ctx.getDryRunBalance().getAsInt()) : api.getWallet().getAvailableBalance();
    }

    static Function<Investor, Collection<Investment>> getInvestingFunction(final Configuration ctx,
                                                                           final List<Loan> availableLoans) {
        final boolean useStrategy = ctx.getInvestmentStrategy().isPresent();
        // figure out what to execute
        return useStrategy ? i -> i.invest(ctx.getInvestmentStrategy().get(), availableLoans) : i -> {
            final Optional<Investment> optional = i.invest(ctx.getLoanId().getAsInt(), ctx.getLoanAmount().getAsInt());
            return (optional.isPresent()) ? Collections.singletonList(optional.get()) : Collections.emptyList();
        };
    }

    static Collection<Investment> invest(final Configuration ctx, final ZonkyApi zonky,
                                         final List<Loan> availableLoans) {
        final BigDecimal balance = Remote.getAvailableBalance(ctx, zonky);
        final Investor i = new Investor(zonky, balance);
        final Collection<Investment> result = Remote.getInvestingFunction(ctx, availableLoans).apply(i);
        return Collections.unmodifiableCollection(result);
    }

    private static OffsetDateTime findEndOfClosedSeason(final Loan l, final Configuration ctx) {
        return l.getDatePublished().plus(ctx.getCaptchaDelayInSeconds(), ChronoUnit.SECONDS);
    }

    private final AuthenticationHandler auth;
    private final Configuration ctx;

    public Remote(final Configuration ctx, final AuthenticationHandler authenticationHandler) {
        this.ctx = ctx;
        this.auth = authenticationHandler;
    }

    Optional<Collection<Investment>> execute(final ApiProvider apiProvider) {
        // check marketplace for loans
        EventRegistry.fire(new MarketplaceCheckStartedEvent());
        final Activity activity = new Activity(this.ctx, apiProvider.cache(), Remote.MARKETPLACE_TIMESTAMP);
        final List<Loan> captchaProtected = activity.getUnactionableLoans();
        captchaProtected.forEach(l ->
                EventRegistry.fire(new CaptchaProtectedLoanArrivalEvent(l, Remote.findEndOfClosedSeason(l, this.ctx)))
        );
        final List<Loan> unprotected = Remote.getAvailableLoans(this.ctx, activity);
        unprotected.forEach(l -> EventRegistry.fire(new UnprotectedLoanArrivalEvent(l)));
        EventRegistry.fire(new MarketplaceCheckCompleteEvent());
        if (unprotected.isEmpty()) { // let's fall asleep
            return Optional.of(Collections.emptyList());
        }
        // start the core investing algorithm
        EventRegistry.fire(new ExecutionStartedEvent(unprotected));
        final Optional<Collection<Investment>> optionalResult =
                this.auth.execute(apiProvider, api -> Remote.invest(this.ctx, api, unprotected));
        activity.settle(); // only settle the marketplace activity when we're sure the app is no longer likely to fail
        if (optionalResult.isPresent()) {
            final Collection<Investment> result = optionalResult.get();
            EventRegistry.fire(new ExecutionCompleteEvent(result));
            Remote.LOGGER.info("RoboZonky {}invested into {} loans.", apiProvider.isDryRun() ? "would have " : "",
                    result.size());
            return Optional.of(result);
        } else {
            EventRegistry.fire(new ExecutionCompleteEvent(Collections.emptyList()));
            return Optional.empty();
        }
    }

    @Override
    public Optional<Collection<Investment>> call() {
        final boolean isDryRun = this.ctx.isDryRun();
        if (isDryRun) {
            Remote.LOGGER.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        try (final ApiProvider apiProvider = new ApiProvider(isDryRun)) { // auto-close the API clients
            return this.execute(apiProvider);
        }
    }

}
