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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.ApiProvider;
import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
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

    static Optional<File> storeInvestmentsMade(final Collection<Investment> result, final boolean dryRun) {
        final String suffix = dryRun ? "dry" : "invested";
        final LocalDateTime now = LocalDateTime.now();
        final String filename =
                "robozonky." + DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(now) + '.' + suffix;
        return Remote.storeInvestmentsMade(new File(filename), result);
    }

    static Optional<File> storeInvestmentsMade(final File target, final Collection<Investment> result) {
        if (result.size() == 0) {
            return Optional.empty();
        }
        final Collection<String> output = result.stream()
                .map(i -> "#" + i.getLoanId() + ": " + i.getAmount() + " CZK")
                .collect(Collectors.toList());
        try {
            Files.write(target.toPath(), output);
            Remote.LOGGER.info("Investments made by RoboZonky during the session were stored in file '{}'.",
                    target.getAbsolutePath());
            return Optional.of(target);
        } catch (final IOException ex) {
            Remote.LOGGER.warn("Failed writing out the list of investments made in this session.", ex);
            return Optional.empty();
        }
    }

    static Collection<Loan> getAvailableLoans(final AppContext ctx, final Activity activity) {
        final boolean shouldSleep = activity.shouldSleep();
        if (shouldSleep) {
            Remote.LOGGER.info("RoboZonky is asleep as there is nothing going on.");
            return Collections.emptyList();
        } else {
            Remote.LOGGER.info("Ignoring loans published earlier than {} seconds ago.", ctx.getCaptchaDelayInSeconds());
            return Collections.unmodifiableList(activity.getAvailableLoans());
        }
    }

    static BigDecimal getAvailableBalance(final AppContext ctx, final ZonkyApi api) {
        final int dryRunInitialBalance = ctx.getDryRunBalance();
        return (ctx.isDryRun() && dryRunInitialBalance >= 0) ?
                BigDecimal.valueOf(dryRunInitialBalance) : api.getWallet().getAvailableBalance();
    }

    static Function<Investor, Collection<Investment>> getInvestingFunction(final AppContext ctx,
                                                                           final Collection<Loan> availableLoans) {
        final boolean useStrategy = ctx.getOperatingMode() == OperatingMode.STRATEGY_DRIVEN;
        // figure out what to execute
        return useStrategy ? i -> i.invest(ctx.getInvestmentStrategy(), availableLoans) : i -> {
            final Optional<Investment> optional = i.invest(ctx.getLoanId(), ctx.getLoanAmount());
            return (optional.isPresent()) ? Collections.singletonList(optional.get()) : Collections.emptyList();
        };
    }

    static Collection<Investment> invest(final AppContext ctx, final ZonkyApi zonky,
                                         final Collection<Loan> availableLoans) {
        final BigDecimal balance = Remote.getAvailableBalance(ctx, zonky);
        final Investor i = new Investor(zonky, balance);
        final Collection<Investment> result = Remote.getInvestingFunction(ctx, availableLoans).apply(i);
        return Collections.unmodifiableCollection(result);
    }

    private final AuthenticationHandler auth;
    private final AppContext ctx;

    public Remote(final AppContext ctx, final AuthenticationHandler authenticationHandler) {
        this.ctx = ctx;
        this.auth = authenticationHandler;
    }

    public Optional<Collection<Investment>> call() {
        final boolean isDryRun = this.ctx.isDryRun();
        if (isDryRun) {
            Remote.LOGGER.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        final ApiProvider apiProvider = new ApiProvider(); // prepare fresh API factory
        final Activity activity = new Activity(this.ctx, apiProvider.cache(), Remote.MARKETPLACE_TIMESTAMP);
        final Collection<Loan> loans = Remote.getAvailableLoans(this.ctx, activity);
        if (loans.isEmpty()) { // let's fall asleep
            return Optional.of(Collections.emptyList());
        }
        final Optional<Collection<Investment>> optionalResult =
                this.auth.execute(apiProvider, api -> Remote.invest(this.ctx, api, loans));
        activity.settle(); // only settle the marketplace activity when we're sure the app is no longer likely to fail
        if (optionalResult.isPresent()) {
            final Collection<Investment> result = optionalResult.get();
            Remote.storeInvestmentsMade(result, isDryRun);
            Remote.LOGGER.info("RoboZonky {}invested into {} loans.", isDryRun ? "would have " : "", result.size());
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

}
