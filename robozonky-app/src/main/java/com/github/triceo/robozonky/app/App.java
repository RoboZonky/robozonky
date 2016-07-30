/*
 *
 *  Copyright 2016 Lukáš Petrovický
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.triceo.robozonky.app;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.util.UnrecoverableRoboZonkyException;
import com.github.triceo.robozonky.app.version.VersionCheck;
import com.github.triceo.robozonky.authentication.Authentication;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.ZonkyApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static void exit(final ReturnCode returnCode, final Future<String> versionFuture) {
        App.newerRoboZonkyVersionExists(versionFuture);
        App.LOGGER.debug("RoboZonky terminating with '{}' return code.", returnCode);
        System.exit(returnCode.getCode());
    }

    static Optional<AppContext> processCommandLine(final String... args) {
        final Optional<CommandLineInterface> optionalCli = CommandLineInterface.parse(args);
        if (!optionalCli.isPresent()) {
            return Optional.empty();
        }
        final CommandLineInterface cli = optionalCli.get();
        final Optional<AuthenticationHandler> auth = new AuthenticationHandlerProvider().apply(cli);
        if (!auth.isPresent()) {
            return Optional.empty();
        }
        return cli.getCliOperatingMode().setup(cli, auth.get());
    }

    private static void core(final AppContext ctx) throws UnrecoverableRoboZonkyException {
        App.LOGGER.info("===== RoboZonky at your service! =====");
        final boolean isDryRun = ctx.isDryRun();
        if (isDryRun) {
            App.LOGGER.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        final Collection<Investment> result = App.invest(ctx); // perform the investing operations
        App.storeInvestmentsMade(result, isDryRun);
        App.LOGGER.info("RoboZonky {}invested into {} loans.", isDryRun ? "would have " : "", result.size());
        App.LOGGER.info("===== RoboZonky out. =====");
    }

    public static void main(final String... args) {
        App.LOGGER.info("RoboZonky v{} loading.", VersionCheck.retrieveCurrentVersion());
        App.LOGGER.debug("Running {} Java v{} on {} v{} ({}).", System.getProperty("java.vendor"),
                System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.version"),
                System.getProperty("os.arch"));
        final Future<String> latestVersion = VersionCheck.retrieveLatestVersion();
        try {
            final Optional<AppContext> optionalCtx = App.processCommandLine(args);
            if (optionalCtx.isPresent()) {
                App.core(optionalCtx.get());
                App.exit(ReturnCode.OK, latestVersion);
            } else {
                App.exit(ReturnCode.ERROR_WRONG_PARAMETERS, latestVersion);
            }
        } catch (final UnrecoverableRoboZonkyException ex) {
            App.LOGGER.error("Application encountered an error during setup." , ex);
            App.exit(ReturnCode.ERROR_SETUP, latestVersion);
        } catch (final RuntimeException ex) {
            App.LOGGER.error("Unexpected error." , ex);
            App.exit(ReturnCode.ERROR_UNEXPECTED, latestVersion);
        }
    }

    /**
     * Check the current version against a different version. Print log message with results.
     * @param futureVersion Version to check against.
     * @return True when a more recent version was found.
     */
    static boolean newerRoboZonkyVersionExists(final Future<String> futureVersion) {
        try {
            final String version = futureVersion.get();
            final boolean hasNewer = VersionCheck.isCurrentVersionOlderThan(version);
            if (hasNewer) {
                App.LOGGER.info("You are using an obsolete version of RoboZonky. Please upgrade to version {}.", version);
                return true;
            } else {
                App.LOGGER.info("Your version of RoboZonky seems up to date.");
                return false;
            }
        } catch (final InterruptedException | ExecutionException ex) {
            App.LOGGER.trace("Version check failed.", ex);
            return false;
        }
    }

    static Optional<File> storeInvestmentsMade(final Collection<Investment> result, final boolean dryRun) {
        final String suffix = dryRun ? "dry" : "invested";
        final LocalDateTime now = LocalDateTime.now();
        final String filename =
                "robozonky." + DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(now) + '.' + suffix;
        return App.storeInvestmentsMade(new File(filename), result);
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
            App.LOGGER.info("Investments made by RoboZonky during the session were stored in file '{}'.",
                    target.getAbsolutePath());
            return Optional.of(target);
        } catch (final IOException ex) {
            App.LOGGER.warn("Failed writing out the list of investments made in this session.", ex);
            return Optional.empty();
        }
    }

    static BigDecimal getAvailableBalance(final AppContext ctx, final ZonkyApi api) {
        final int dryRunInitialBalance = ctx.getDryRunBalance();
        return (ctx.isDryRun() && dryRunInitialBalance >= 0) ?
                BigDecimal.valueOf(dryRunInitialBalance) : api.getWallet().getAvailableBalance();
    }

    static Function<Investor, Collection<Investment>> getInvestingFunction(final AppContext ctx) {
        final boolean useStrategy = ctx.getOperatingMode() == OperatingMode.STRATEGY_DRIVEN;
        // figure out what to execute
        return useStrategy ? Investor::invest : i -> {
            final Optional<Investment> optional = i.invest(ctx.getLoanId(), ctx.getLoanAmount());
            return (optional.isPresent()) ? Collections.singletonList(optional.get()) : Collections.emptyList();
        };
    }

    private static Collection<Investment> invest(final AppContext ctx) throws UnrecoverableRoboZonkyException {
        final AuthenticationHandler handler = ctx.getAuthenticationHandler();
        final Authentication login = handler.login();
        try { // execute the investment
            final BigDecimal balance = App.getAvailableBalance(ctx, login.getZonkyApi());
            final Investor i = new Investor(login.getZonkyApi(), login.getZotifyApi(), ctx.getInvestmentStrategy(),
                    balance);
            return App.getInvestingFunction(ctx).apply(i);
        } finally { // make sure logout is processed at all costs
            handler.logout(login);
        }
    }

}
