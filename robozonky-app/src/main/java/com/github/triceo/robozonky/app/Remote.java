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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.ApiProvider;
import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.ZonkyProxy;
import com.github.triceo.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.triceo.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.triceo.robozonky.api.notifications.LoanArrivedEvent;
import com.github.triceo.robozonky.api.notifications.MarketplaceCheckCompletedEvent;
import com.github.triceo.robozonky.api.notifications.MarketplaceCheckStartedEvent;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.configuration.Configuration;
import com.github.triceo.robozonky.notifications.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base investing algorithm, ready to be executed with {@link ScheduledExecutorService}.
 */
public class Remote implements Callable<Optional<Collection<Investment>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Remote.class);

    static Optional<Collection<Loan>> getAvailableLoans(final Activity activity) {
        final boolean shouldSleep = activity.shouldSleep();
        if (shouldSleep) {
            Remote.LOGGER.info("RoboZonky is asleep as there is nothing going on.");
            return Optional.empty();
        } else {
            return Optional.of(Stream.concat(activity.getUnactionableLoans().stream(), activity.getAvailableLoans()
                    .stream())
                    .distinct()
                    .collect(Collectors.toList()));
        }
    }

    static BigDecimal getAvailableBalance(final Configuration ctx, final ZonkyProxy api) {
        return (ctx.isDryRun() && ctx.getDryRunBalance().isPresent()) ?
                BigDecimal.valueOf(ctx.getDryRunBalance().getAsInt()) :
                api.execute(zonky -> zonky.getWallet().getAvailableBalance());
    }

    static Collection<Investment> invest(final Configuration ctx, final ZonkyApi api,
                                         final Function<Investor, Collection<Investment>> investingFunction) {
        final ZonkyProxy proxy = ctx.getZonkyProxyBuilder().build(api);
        final BigDecimal balance = Remote.getAvailableBalance(ctx, proxy);
        return Collections.unmodifiableCollection(investingFunction.apply(new Investor(proxy, balance)));
    }

    private final AuthenticationHandler auth;
    private final Configuration ctx;

    public Remote(final Configuration ctx, final AuthenticationHandler authenticationHandler) {
        this.ctx = ctx;
        this.auth = authenticationHandler;
    }

    Optional<Collection<Investment>> executeStrategy(final ApiProvider apiProvider) {
        // check marketplace for loans
        Events.fire(new MarketplaceCheckStartedEvent());
        final Activity activity = new Activity(this.ctx, apiProvider.cache());
        final Optional<Collection<Loan>> availableLoans = Remote.getAvailableLoans(activity);
        Events.fire(new MarketplaceCheckCompletedEvent());
        if (!availableLoans.isPresent()) { // sleep
            return Optional.of(Collections.emptyList());
        }
        final List<LoanDescriptor> loans = availableLoans.get().stream()
                .map(l -> new LoanDescriptor(l, this.ctx.getCaptchaDelay()))
                .peek(l -> Events.fire(new LoanArrivedEvent(l)))
                .collect(Collectors.toList());
        // start the core investing algorithm
        final Function<Investor, Collection<Investment>> investor = i -> {
            Events.fire(new ExecutionStartedEvent(loans, i.getBalance().intValue()));
            final Collection<Investment> result = i.invest(this.ctx.getInvestmentStrategy().get(), loans);
            Events.fire(new ExecutionCompletedEvent(result, i.getBalance().intValue()));
            return result;
        };
        final Optional<Collection<Investment>> optionalResult =
                this.auth.execute(apiProvider, api -> Remote.invest(this.ctx, api, investor));
        activity.settle(); // only settle the marketplace activity when we're sure the app is no longer likely to fail
        return optionalResult;
    }

    Optional<Collection<Investment>> executeSingleInvestment(final ApiProvider apiProvider) {
        final Function<Investor, Collection<Investment>> investor = i -> {
            final Optional<Investment> optional = i.invest(this.ctx.getLoanId().getAsInt(),
                    this.ctx.getLoanAmount().getAsInt(), this.ctx.getCaptchaDelay());
            return (optional.isPresent()) ? Collections.singletonList(optional.get()) : Collections.emptyList();
        };
        return this.auth.execute(apiProvider, api -> Remote.invest(this.ctx, api, investor));
    }

    @Override
    public Optional<Collection<Investment>> call() {
        if (this.ctx.isDryRun()) {
            this.ctx.getZonkyProxyBuilder().asDryRun();
            Remote.LOGGER.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        try (final ApiProvider apiProvider = new ApiProvider()) { // auto-close the API clients
            final Optional<Collection<Investment>> optionalResult = this.ctx.getInvestmentStrategy().isPresent() ?
                    this.executeStrategy(apiProvider) :
                    this.executeSingleInvestment(apiProvider);
            optionalResult.ifPresent(r -> Remote.LOGGER.info("RoboZonky {}invested into {} loans.",
                    this.ctx.isDryRun() ? "would have " : "", r.size()));
            return optionalResult;
        }
    }

}
