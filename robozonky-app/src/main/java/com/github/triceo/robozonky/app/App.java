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
import java.security.KeyStoreException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SensitiveInformationProvider;
import com.github.triceo.robozonky.app.util.KeyStoreHandler;
import com.github.triceo.robozonky.app.version.VersionCheck;
import com.github.triceo.robozonky.authentication.Authentication;
import com.github.triceo.robozonky.authentication.Authenticator;
import com.github.triceo.robozonky.operations.LoginOperation;
import com.github.triceo.robozonky.operations.LogoutOperation;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.ZonkyApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static final File DEFAULT_KEYSTORE_FILE = new File("robozonky.keystore");

    private static void exit(final ReturnCode returnCode, final Future<String> versionFuture) {
        App.versionCheck(versionFuture);
        App.LOGGER.debug("RoboZonky terminating with '{}' return code.", returnCode);
        System.exit(returnCode.getCode());
    }

    static Optional<SensitiveInformationProvider> getSensitiveInformationProvider(final CommandLineInterface cli,
                                                                                  final File defaultKeyStore) {
        final Optional<File> keyStoreLocation = cli.getKeyStoreLocation();
        if (keyStoreLocation.isPresent()) { // if user requests keystore, cli is only used to retrieve keystore file
            final File store = keyStoreLocation.get();
            try {
                final KeyStoreHandler ksh = KeyStoreHandler.open(store, cli.getPassword());
                return Optional.of(SensitiveInformationProvider.keyStoreBased(ksh));
            } catch (final IOException | KeyStoreException ex) {
                App.LOGGER.error("Failed opening guarded storage.", ex);
                return Optional.empty();
            }
        } else { // else everything is read from the cli and put into a keystore
            try {
                final Optional<String> usernameProvided = cli.getUsername();
                final boolean usernamePresent = usernameProvided.isPresent();
                final boolean storageExists = defaultKeyStore.canRead();
                if (storageExists) {
                    if (defaultKeyStore.delete()) {
                        App.LOGGER.debug("Deleted pre-existing guarded storage.");
                    } else {
                        App.LOGGER.error("Stale guarded storage is present and can not be deleted.");
                        return Optional.empty();
                    }
                }
                final KeyStoreHandler ksh = KeyStoreHandler.create(defaultKeyStore, cli.getPassword());
                if (!usernamePresent) {
                    App.LOGGER.error("When not using guarded storage, username must be provided.");
                    return Optional.empty();
                } else if (storageExists) {
                    App.LOGGER.info("Using plain-text credentials when guarded storage available. Consider switching.");
                } else {
                    App.LOGGER.info("Guarded storage has been created with your username and password: {}",
                            defaultKeyStore);
                }
                return Optional.of(SensitiveInformationProvider.keyStoreBased(ksh, usernameProvided.get(),
                        cli.getPassword()));
            } catch (final IOException | KeyStoreException ex) {
                App.LOGGER.error("Failed reading guarded storage.", ex);
                return Optional.empty();
            }
        }
    }

    private static Optional<AuthenticationHandler> getAuthenticationMethod(final CommandLineInterface cli) {
        final boolean useToken = cli.isTokenEnabled();
        final Optional<SensitiveInformationProvider> optionalSensitive =
                App.getSensitiveInformationProvider(cli, App.DEFAULT_KEYSTORE_FILE);
        if (!optionalSensitive.isPresent()) {
            return Optional.empty();
        }
        final SensitiveInformationProvider sensitive = optionalSensitive.get();
        final AuthenticationHandler auth = useToken ? AuthenticationHandler.tokenBased(sensitive)
                : AuthenticationHandler.passwordBased(sensitive);
        final Optional<Integer> secs = cli.getTokenRefreshBeforeExpirationInSeconds();
        if (secs.isPresent()) {
            auth.withTokenRefreshingBeforeExpiration(secs.get(), ChronoUnit.SECONDS);
        }
        return Optional.of(auth);
    }

    static Optional<AppContext> processCommandLine(final String... args) {
        final Optional<CommandLineInterface> optionalCli = CommandLineInterface.parse(args);
        if (!optionalCli.isPresent()) {
            return Optional.empty();
        }
        final CommandLineInterface cli = optionalCli.get();
        final Optional<OperatingMode> om = cli.getCliOperatingMode();
        if (!om.isPresent()) {
            return Optional.empty();
        }
        final Optional<AuthenticationHandler> auth = App.getAuthenticationMethod(cli);
        if (!auth.isPresent()) {
            return Optional.empty();
        }
        return om.get().setup(cli, auth.get());
    }

    public static void main(final String... args) {
        App.LOGGER.info("RoboZonky v{} loading.", VersionCheck.retrieveCurrentVersion());
        final Future<String> latestVersion = VersionCheck.retrieveLatestVersion();
        try {
            final Optional<AppContext> optionalCtx = App.processCommandLine(args);
            if (!optionalCtx.isPresent()) {
                App.exit(ReturnCode.ERROR_WRONG_PARAMETERS, latestVersion);
            }
            final AppContext ctx = optionalCtx.get();
            App.LOGGER.info("===== RoboZonky at your service! =====");
            final boolean isDryRun = ctx.isDryRun();
            if (isDryRun) {
                App.LOGGER.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
            }
            final Collection<Investment> result = App.invest(ctx); // perform the investing operations
            App.storeInvestmentsMade(result, isDryRun);
            App.LOGGER.info("RoboZonky {}invested into {} loans.", isDryRun ? "would have " : "", result.size());
        } catch (final UnrecoverableRoboZonkyException ex) {
            App.LOGGER.error("Application encountered an error during setup." , ex);
            App.exit(ReturnCode.ERROR_SETUP, latestVersion);
            return;
        } catch (final RuntimeException ex) {
            App.LOGGER.error("Unexpected error." , ex);
            App.exit(ReturnCode.ERROR_UNEXPECTED, latestVersion);
            return;
        }
        App.LOGGER.info("===== RoboZonky out. =====");
        App.exit(ReturnCode.OK, latestVersion);
    }

    /**
     * Check the current version against a different version. Print log message with results.
     * @param futureVersion Version to check against.
     */
    private static void versionCheck(final Future<String> futureVersion) {
        try {
            final String version = futureVersion.get();
            final boolean hasNewer = VersionCheck.isCurrentVersionOlderThan(version);
            if (hasNewer) {
                App.LOGGER.info("You are using an obsolete version of RoboZonky. Please upgrade to version {}.", version);
            } else {
                App.LOGGER.info("Your version of RoboZonky seems up to date.");
            }
        } catch (final InterruptedException | ExecutionException ex) {
            App.LOGGER.trace("Version check failed.", ex);
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
        // log in
        final AuthenticationHandler handler = ctx.getAuthenticationHandler();
        final Authenticator auth = handler.build(ctx.isDryRun());
        final Optional<Authentication> possibleLogin = new LoginOperation().apply(auth);
        if (!possibleLogin.isPresent()) {
            throw new UnrecoverableRoboZonkyException("Login failed.");
        }
        final Authentication login = possibleLogin.get();
        final boolean logoutAllowed = handler.processToken(login.getZonkyApiToken());
        try { // execute the investment
            final BigDecimal balance = App.getAvailableBalance(ctx, login.getZonkyApi());
            final Investor i = new Investor(login.getZonkyApi(), login.getZotifyApi(), ctx.getInvestmentStrategy(),
                    balance);
            return App.getInvestingFunction(ctx).apply(i);
        } finally { // make sure logout is processed at all costs
            if (logoutAllowed) {
                new LogoutOperation().apply(login.getZonkyApi());
            } else { // if we're using the token, we should never log out
                App.LOGGER.info("Refresh token needs to be reused, not logging out of Zonky.");
            }
        }
    }

}
