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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.ApiProvider;
import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZotifyApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Remote implements Callable<Optional<Collection<Investment>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Remote.class);

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

    static Collection<Loan> getAvailableLoans(final ZotifyApi api, final int delay) {
        Remote.LOGGER.info("Ignoring loans published earlier than {} seconds ago.", delay);
        return api.getLoans().stream()
                .filter(l -> Instant.now().isAfter(l.getDatePublished().plus(delay, ChronoUnit.SECONDS)))
                .collect(Collectors.toList());
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
        return Remote.getInvestingFunction(ctx, availableLoans).apply(i);
    }

    private final AuthenticationHandler auth;
    private final AppContext ctx;

    public Remote(final AppContext ctx, final AuthenticationHandler authenticationHandler) {
        this.ctx = ctx;
        this.auth = authenticationHandler;
    }

    /**
     * Core investing algorithm. Will log in, invest and log out.
     *
     * @return True if login succeeded and the algorithm moved over to investing.
     * @throws RuntimeException Any exception on login and logout will be caught and logged, therefore any runtime
     * exception thrown is a problem during the investing operation itself.
     */
    public Optional<Collection<Investment>> call() {
        final boolean isDryRun = this.ctx.isDryRun();
        if (isDryRun) {
            Remote.LOGGER.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        final ApiProvider apiProvider = new ApiProvider();
        final Collection<Loan> loans = Remote.getAvailableLoans(apiProvider.cache(), ctx.getCaptchaDelayInSeconds());
        final Optional<Collection<Investment>> optionalResult =
                this.auth.execute(apiProvider, api -> Remote.invest(this.ctx, api, loans));
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
