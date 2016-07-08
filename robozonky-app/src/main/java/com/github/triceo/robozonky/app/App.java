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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyStoreException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SensitiveInformationProvider;
import com.github.triceo.robozonky.app.util.KeyStoreHandler;
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

    static boolean RUNNING_OUTSIDE_TESTS = true; // purely for testing purposes

    static void exit(final ReturnCode returnCode) {
        if (App.RUNNING_OUTSIDE_TESTS) {
            App.LOGGER.debug("RoboZonky terminating with '{}' return code.", returnCode);
            System.exit(returnCode.getCode());
        } else {
            throw new RoboZonkyTestingExitException("Return code: " + returnCode);
        }
    }

    static SensitiveInformationProvider getSensitiveInformationProvider(final CommandLineInterface cli,
                                                                        final File defaultKeyStore) {
        final Optional<File> keyStoreLocation = cli.getKeyStoreLocation();
        if (keyStoreLocation.isPresent()) { // if user requests keystore, cli is only used to retrieve keystore file
            final File store = keyStoreLocation.get();
            try {
                final KeyStoreHandler ksh = KeyStoreHandler.open(store, cli.getPassword());
                return SensitiveInformationProvider.keyStoreBased(ksh);
            } catch (final IOException | KeyStoreException ex) {
                cli.printHelpAndExit("Failed opening guarded storage.", ex);
                return null;
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
                        throw new IllegalArgumentException("Stale guarded storage is present and can not be deleted.");
                    }
                }
                final KeyStoreHandler ksh = KeyStoreHandler.create(defaultKeyStore, cli.getPassword());
                if (!usernamePresent) {
                    cli.printHelpAndExit("When not using guarded storage, username must be provided.", true);
                } else if (storageExists) {
                    App.LOGGER.info("Using plain-text credentials when guarded storage available. Consider switching.");
                } else {
                    App.LOGGER.info("Guarded storage has been created with your username and password: {}",
                            defaultKeyStore);
                }
                return SensitiveInformationProvider.keyStoreBased(ksh, usernameProvided.get(), cli.getPassword());
            } catch (final IOException | KeyStoreException ex) {
                cli.printHelpAndExit("Failed reading guarded storage.", ex);
                return null;
            }
        }
    }

    private static AuthenticationHandler getAuthenticationMethod(final CommandLineInterface cli) {
        final boolean useToken = cli.isTokenEnabled();
        final SensitiveInformationProvider sensitive =
                App.getSensitiveInformationProvider(cli, App.DEFAULT_KEYSTORE_FILE);
        final AuthenticationHandler auth = useToken ? AuthenticationHandler.tokenBased(sensitive)
                : AuthenticationHandler.passwordBased(sensitive);
        final Optional<Integer> secs = cli.getTokenRefreshBeforeExpirationInSeconds();
        if (secs.isPresent()) {
            auth.withTokenRefreshingBeforeExpiration(secs.get(), ChronoUnit.SECONDS);
        }
        return auth;
    }

    static AppContext processCommandLine(final String... args) {
        final CommandLineInterface cli = CommandLineInterface.parse(args).orElse(null); // null will never happen
        return cli.getCliOperatingMode().orElseGet(() -> {
            cli.printHelpAndExit("", false);
            return null; // will never get here
        }).setup(cli, App.getAuthenticationMethod(cli)).orElse(null); // null should never be returned
    }

    public static void main(final String... args) {
        App.LOGGER.info("RoboZonky v{} loading.", App.class.getPackage().getImplementationVersion());
        final AppContext ctx = App.processCommandLine(args);
        App.LOGGER.info("===== RoboZonky at your service! =====");
        try {
            final boolean isDryRun = ctx.isDryRun();
            if (isDryRun) {
                App.LOGGER.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
            }
            final Collection<Investment> result = App.invest(ctx); // perform the investing operations
            App.storeInvestmentsMade(result, isDryRun);
            App.LOGGER.info("RoboZonky {}invested into {} loans.", isDryRun ? "would have " : "", result.size());
        } catch (final RuntimeException ex) {
            App.LOGGER.error("Unexpected error." , ex);
            App.exit(ReturnCode.ERROR_UNEXPECTED);
            return; // non-tests will not get here
        }
        App.LOGGER.info("===== RoboZonky out. =====");
        App.exit(ReturnCode.OK);
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
        try (final BufferedWriter bw = Files.newBufferedWriter(target.toPath(), Charset.forName("UTF-8"))) {
            for (final Investment i : result) {
                bw.write("#" + i.getLoanId() + ": " + i.getAmount() + " CZK");
                bw.newLine();
            }
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

    private static Collection<Investment> invest(final AppContext ctx) {
        // log in
        final AuthenticationHandler handler = ctx.getAuthenticationHandler();
        final Authenticator auth = handler.build(ctx.isDryRun());
        final Optional<Authentication> possibleLogin = new LoginOperation().apply(auth);
        if (!possibleLogin.isPresent()) {
            App.exit(ReturnCode.ERROR_LOGIN);
            return Collections.emptyList(); // should never get here
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
