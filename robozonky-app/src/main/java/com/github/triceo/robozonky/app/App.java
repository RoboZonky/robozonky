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

import com.github.triceo.robozonky.Operations;
import com.github.triceo.robozonky.OperationsContext;
import com.github.triceo.robozonky.Util;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SensitiveInformationProvider;
import com.github.triceo.robozonky.app.util.KeyStoreHandler;
import com.github.triceo.robozonky.authentication.Authenticator;
import com.github.triceo.robozonky.exceptions.LoginFailedException;
import com.github.triceo.robozonky.exceptions.LogoutFailedException;
import com.github.triceo.robozonky.remote.Investment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.triceo.robozonky.app.OperatingMode.STRATEGY_DRIVEN;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static final File DEFAULT_KEYSTORE_FILE = new File("robozonky.keystore");

    static boolean PERFORM_SYSTEM_EXIT = true; // purely for testing purposes

    static void exit(final ReturnCode returnCode) {
        if (App.PERFORM_SYSTEM_EXIT) {
            App.LOGGER.debug("RoboZonky terminating with '{}' return code.", returnCode);
            System.exit(returnCode.getCode());
        } else {
            throw new RoboZonkyTestingExitException("Return code: " + returnCode);
        }
    }

    private static AppContext prepareStrategyDrivenMode(final CommandLineInterface cli) {
        final Optional<Integer> loanAmount = cli.getLoanAmount();
        if (loanAmount.isPresent()) {
            cli.printHelpAndExit("Loan amount makes no sense in this context.", true);
        }
        final Optional<String> strategyFilePath = cli.getStrategyConfigurationFilePath();
        if (!strategyFilePath.isPresent()) {
            cli.printHelpAndExit("Strategy file must be provided.", true);
        }
        final File strategyConfig = new File(strategyFilePath.get());
        if (!strategyConfig.canRead()) {
            cli.printHelpAndExit("Investment strategy file must be readable.", true);
        }
        final AuthenticationHandler auth = App.getAuthenticationMethod(cli);
        try {
            if (cli.isDryRun()) {
                return new AppContext(auth, StrategyParser.parse(strategyConfig), cli.getDryRunBalance());
            } else {
                return new AppContext(auth, StrategyParser.parse(strategyConfig));
            }
        } catch (final Exception ex) {
            cli.printHelpAndExit("Failed parsing strategy: " + ex.getMessage(), true);
            return null;
        }
    }

    private static SensitiveInformationProvider getSensitiveInformationProvider(final CommandLineInterface cli) {
        final Optional<File> keyStoreLocation = cli.getKeyStoreLocation();
        if (keyStoreLocation.isPresent()) { // if user requests keystore, cli is only used to retrieve keystore file
            final File store = keyStoreLocation.get();
            try {
                final KeyStoreHandler ksh = KeyStoreHandler.open(store, cli.getPassword());
                return SensitiveInformationProvider.keyStoreBased(ksh);
            } catch (final IOException | KeyStoreException ex) {
                cli.printHelpAndExit("Failed secure storage: " + ex.getMessage(), true);
                return null;
            }
        } else { // else everything is read from the cli and put into a keystore
            try {
                final KeyStoreHandler ksh = KeyStoreHandler.create(App.DEFAULT_KEYSTORE_FILE, cli.getPassword());
                App.LOGGER.info("Guarded storage has been created with your username and password: {}",
                        App.DEFAULT_KEYSTORE_FILE);
                App.LOGGER.info("Feel free to use this instead of providing the information on the command line.");
                App.LOGGER.info("Please change the storage password to something else than your Zonky password.");
                return SensitiveInformationProvider.keyStoreBased(ksh, cli.getUsername().get(), cli.getPassword());
            } catch (final IOException | KeyStoreException ex) {
                cli.printHelpAndExit("Failed reading secure storage: " + ex.getMessage(), true);
                return null;
            }
        }
    }

    private static AuthenticationHandler getAuthenticationMethod(final CommandLineInterface cli) {
        final boolean useToken = cli.isTokenEnabled();
        final SensitiveInformationProvider sensitive = App.getSensitiveInformationProvider(cli);
        final AuthenticationHandler auth = useToken ? AuthenticationHandler.tokenBased(sensitive)
                : AuthenticationHandler.passwordBased(sensitive);
        final Optional<Integer> secs = cli.getTokenRefreshBeforeExpirationInSeconds();
        if (secs.isPresent()) {
            auth.withTokenRefreshingBeforeExpiration(secs.get(), ChronoUnit.SECONDS);
        }
        return auth;
    }

    private static AppContext prepareUserDrivenMode(final CommandLineInterface cli) {
        final Optional<Integer> loanId = cli.getLoanId();
        final Optional<Integer> loanAmount = cli.getLoanAmount();
        if (!loanId.isPresent()) {
            cli.printHelpAndExit("Loan ID must be provided.", true);
        } else if (!loanAmount.isPresent()) {
            cli.printHelpAndExit("Loan amount must be provided.", true);
        }
        final AuthenticationHandler auth = App.getAuthenticationMethod(cli);
        if (cli.isDryRun()) {
            return new AppContext(auth, loanId.get(), loanAmount.get(), cli.getDryRunBalance());
        } else {
            return new AppContext(auth, loanId.get(), loanAmount.get());
        }
    }

    private static AppContext processCommandLine(final String... args) {
        final CommandLineInterface cli = CommandLineInterface.parse(args);
        switch (cli.getCliOperatingMode()) {
            case STRATEGY_DRIVEN:
                return App.prepareStrategyDrivenMode(cli);
            case USER_DRIVER:
                return App.prepareUserDrivenMode(cli);
            default:
                cli.printHelpAndExit("", false);
                return null; // just in case of tests
        }
    }

    public static void main(final String... args) {
        App.LOGGER.debug("RoboZonky v{} loading.", Util.getRoboZonkyVersion());
        try {
            App.letsGo(App.processCommandLine(args)); // and start actually working with Zonky
            App.exit(ReturnCode.OK);
        } catch (final Exception ex) {
            App.LOGGER.error("Unexpected error." , ex);
            App.exit(ReturnCode.ERROR_UNEXPECTED);
        }
    }

    private static void storeInvestmentsMade(final Collection<Investment> result, final boolean dryRun) {
        final String suffix = dryRun ? "dry" : "invested";
        final LocalDateTime now = LocalDateTime.now();
        final String filename =
                "robozonky." + DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(now) + '.' + suffix;
        final File target = new File(filename);
        try (final BufferedWriter bw = Files.newBufferedWriter(target.toPath(), Charset.forName("UTF-8"))) {
            for (final Investment i : result) {
                bw.write('#' + i.getLoanId() + ": " + i.getAmount() + " CZK");
                bw.newLine();
            }
            App.LOGGER.info("Investments made by RoboZonky during the session were stored in file '{}'.", filename);
        } catch (final IOException ex) {
            App.LOGGER.warn("Failed writing out the list of investments made in this session.", ex);
        }
    }

    private static void letsGo(final AppContext ctx) {
        App.LOGGER.info("===== RoboZonky at your service! =====");
        final boolean dryRun = ctx.isDryRun();
        if (dryRun) {
            App.LOGGER.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        // figure out whether to invest based on strategy or whether to make a single investment
        final boolean useStrategy = ctx.getOperatingMode() == STRATEGY_DRIVEN;
        final Function<OperationsContext, Collection<Investment>> op = useStrategy ? Operations::invest : oc -> {
            final Optional<Investment> optional = Operations.invest(oc, ctx.getLoanId(), ctx.getLoanAmount());
            if (optional.isPresent()) {
                return Collections.singletonList(optional.get());
            } else {
                return Collections.emptyList();
            }
        };
        // and now perform the selected operation
        final Collection<Investment> result = App.operate(ctx, op);
        if (result.isEmpty()) {
            App.LOGGER.info("RoboZonky did not invest.");
        } else {
            App.storeInvestmentsMade(result, dryRun);
            if (dryRun) {
                App.LOGGER.info("RoboZonky pretended to invest into {} loans.", result.size());
            } else {
                App.LOGGER.info("RoboZonky invested into {} loans.", result.size());
            }
        }
        App.LOGGER.info("===== RoboZonky out. =====");
    }

    private static Collection<Investment> operate(final AppContext ctx,
                                                  final Function<OperationsContext, Collection<Investment>> operations) {
        if (ctx.isDryRun() && ctx.getDryRunBalance() < Operations.MINIMAL_INVESTMENT_ALLOWED) {
            App.LOGGER.info("Starting balance in dry run is lower than minimum, no need to execute at all.");
            return Collections.emptyList();
        }
        final boolean useStrategy = ctx.getOperatingMode() == STRATEGY_DRIVEN;
        try {
            final AuthenticationHandler handler = ctx.getAuthenticationHandler();
            final Authenticator auth = handler.build();
            final OperationsContext oc = useStrategy ?
                    Operations.login(auth, ctx.isDryRun(), ctx.getDryRunBalance(), ctx.getInvestmentStrategy()) :
                    Operations.login(auth, ctx.isDryRun(), ctx.getDryRunBalance());
            final boolean logoutAllowed = handler.processToken(oc.getAuthentication().getApiToken());
            final Collection<Investment> result = operations.apply(oc);
            if (logoutAllowed) { // log out
                try {
                    Operations.logout(oc);
                } catch (final LogoutFailedException ex) {
                    App.LOGGER.warn("Logging out of Zonky failed.", ex);
                }
            } else {  // if we're using the token, we should never log out
                App.LOGGER.info("Refresh token stored, not logging out of Zonky.");
            }
            return result;
        } catch (final LoginFailedException ex) {
            App.LOGGER.error("Logging into Zonky failed. No investments were made.", ex);
            App.exit(ReturnCode.ERROR_LOGIN);
        }
        return Collections.emptyList(); // should never get here
    }

}
