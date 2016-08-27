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
import java.net.SocketException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.version.VersionCheck;
import com.github.triceo.robozonky.authentication.Authentication;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZotifyApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static final ExecutorService HTTP_EXECUTOR = Executors.newWorkStealingPool();

    private static void exit(final ReturnCode returnCode, final Future<String> versionFuture) {
        App.HTTP_EXECUTOR.shutdown();
        App.newerRoboZonkyVersionExists(versionFuture);
        App.LOGGER.debug("RoboZonky terminating with '{}' return code.", returnCode);
        App.LOGGER.info("===== RoboZonky out. =====");
        System.exit(returnCode.getCode());
    }

    /**
     * Core investing algorithm. Will log in, invest and log out.
     *
     * @param ctx
     * @param auth
     * @return True if login succeeded and the algorithm moved over to investing.
     * @throws RuntimeException Any exception on login and logout will be caught and logged, therefore any runtime
     * exception thrown is a problem during the investing operation itself.
     */
    private static boolean core(final AppContext ctx, final AuthenticationHandler auth) {
        App.LOGGER.info("===== RoboZonky at your service! =====");
        final boolean isDryRun = ctx.isDryRun();
        if (isDryRun) {
            App.LOGGER.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        final Authentication login;
        try { // catch this exception here, so that anything coming from the invest() method can be thrown separately
            login = auth.login();
        } catch (final BadRequestException ex) {
            App.LOGGER.error("Failed authenticating with Zonky.", ex);
            return false;
        }
        final Collection<Investment> result = App.invest(ctx, login.getZonkyApi(), login.getZotifyApi());
        try { // log out and ignore any resulting error
            auth.logout(login);
        } catch (final RuntimeException ex) {
            App.LOGGER.warn("Failed logging out of Zonky.", ex);
        }
        App.storeInvestmentsMade(result, isDryRun);
        App.LOGGER.info("RoboZonky {}invested into {} loans.", isDryRun ? "would have " : "", result.size());
        return true;
    }

    public static void main(final String... args) {
        App.LOGGER.info("RoboZonky v{} loading.", VersionCheck.retrieveCurrentVersion());
        App.LOGGER.debug("Running {} Java v{} on {} v{} ({}, {}).", System.getProperty("java.vendor"),
                System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.version"),
                System.getProperty("os.arch"), Locale.getDefault());
        final Future<String> latestVersion = VersionCheck.retrieveLatestVersion(App.HTTP_EXECUTOR);
        boolean faultTolerant = false;
        try {
            final Optional<CommandLineInterface> optionalCli = CommandLineInterface.parse(args);
            if (!optionalCli.isPresent()) {
                App.exit(ReturnCode.ERROR_WRONG_PARAMETERS, latestVersion);
            }
            final CommandLineInterface cli = optionalCli.get();
            faultTolerant = cli.isFaultTolerant();
            if (faultTolerant) {
                App.LOGGER.info("RoboZonky is in fault-tolerant mode. Certain errors may not be reported as such.");
            }
            final Optional<AppContext> optionalCtx = cli.getCliOperatingMode().setup(cli);
            if (!optionalCtx.isPresent()) {
                App.exit(ReturnCode.ERROR_WRONG_PARAMETERS, latestVersion);
            }
            final Optional<AuthenticationHandler> optionalAuth = new AuthenticationHandlerProvider().apply(cli);
            if (!optionalAuth.isPresent()) {
                App.exit(ReturnCode.ERROR_SETUP, latestVersion);
            }
            final boolean loginSucceeded = App.core(optionalCtx.get(), optionalAuth.get());
            if (!loginSucceeded) {
                App.exit(ReturnCode.ERROR_SETUP, latestVersion);
            } else {
                App.exit(ReturnCode.OK, latestVersion);
            }
        } catch (final ProcessingException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof SocketException) {
                App.handleZonkyMaintenanceError(ex, latestVersion, faultTolerant);
            } else {
                App.handleUnexpectedError(ex, latestVersion);
            }
        } catch (final NotAllowedException ex) {
            App.handleZonkyMaintenanceError(ex, latestVersion, faultTolerant);
        } catch (final WebApplicationException ex) {
            App.LOGGER.error("Application encountered remote API error.", ex);
            App.exit(ReturnCode.ERROR_REMOTE, latestVersion);
        } catch (final RuntimeException ex) {
            App.handleUnexpectedError(ex, latestVersion);
        }
    }

    private static void handleUnexpectedError(final RuntimeException ex, final Future<String> latestVersion) {
        App.LOGGER.error("Unexpected error, likely RoboZonky bug." , ex);
        App.exit(ReturnCode.ERROR_UNEXPECTED, latestVersion);
    }

    private static void handleZonkyMaintenanceError(final RuntimeException ex, final Future<String> latestVersion,
                                                    final boolean faultTolerant) {
        App.LOGGER.warn("Application not allowed to access remote API, Zonky likely down for maintenance.", ex);
        App.exit(faultTolerant ? ReturnCode.OK : ReturnCode.ERROR_DOWN, latestVersion);
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

    static Collection<Investment> invest(final AppContext ctx, final ZonkyApi zonky, final ZotifyApi zotify) {
        final BigDecimal balance = App.getAvailableBalance(ctx, zonky);
        final Investor i = new Investor(zonky, zotify, ctx.getInvestmentStrategy(), balance);
        return App.getInvestingFunction(ctx).apply(i);
    }

}
