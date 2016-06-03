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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import javax.xml.bind.JAXBException;

import com.github.triceo.robozonky.Operations;
import com.github.triceo.robozonky.OperationsContext;
import com.github.triceo.robozonky.Util;
import com.github.triceo.robozonky.authentication.AuthenticationMethod;
import com.github.triceo.robozonky.exceptions.LoginFailedException;
import com.github.triceo.robozonky.exceptions.LogoutFailedException;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static final File TOKEN_FILE = new File("robozonky.token");

    static final String EXIT_ON_HELP = "robozonky.do.not.exit";
    static final int REFRESH_TOKEN_BEFORE_EXPIRATION_SECONDS = 60;

    private static void exit(final ReturnCode returnCode) {
        App.LOGGER.debug("RoboZonky terminating with '{}' return code.", returnCode);
        System.exit(returnCode.getCode());
    }

    private static void printHelpAndExit(final CommandLineInterface cli, final String message, final boolean exitWithError) {
        cli.printHelpAndExit(message, exitWithError);
        if (!System.getProperty(App.EXIT_ON_HELP, "false").equals("true")) {
            App.exit(exitWithError ? ReturnCode.ERROR_WRONG_PARAMETERS : ReturnCode.OK);
        }
    }

    private static AppContext prepareStrategyDrivenMode(final CommandLineInterface cli) {
        final Optional<Integer> loanAmount = cli.getLoanAmount();
        if (loanAmount.isPresent()) {
            App.printHelpAndExit(cli, "Loan amount makes no sense in this context.", true);
        }
        final Optional<String> strategyFilePath = cli.getStrategyConfigurationFilePath();
        if (!strategyFilePath.isPresent()) {
            App.printHelpAndExit(cli, "Strategy file must be provided.", true);
        }
        final File strategyConfig = new File(strategyFilePath.get());
        if (!strategyConfig.canRead()) {
            App.printHelpAndExit(cli, "Investment strategy file must be readable.", true);
        }
        final AuthenticationMethod auth = App.getAuthenticationMethod(cli);
        try {
            if (cli.isDryRun()) {
                return new AppContext(auth, cli.isTokenEnabled(), StrategyParser.parse(strategyConfig),
                        cli.getDryRunBalance());
            } else {
                return new AppContext(auth, cli.isTokenEnabled(), StrategyParser.parse(strategyConfig));
            }
        } catch (final Exception e) {
            App.printHelpAndExit(cli, "Failed parsing strategy: " + e.getMessage(), true);
            return null;
        }
    }

    private static AuthenticationMethod getAuthenticationMethod(final CommandLineInterface cli) {
        final Optional<String> username = cli.getUsername();
        final Optional<String> password = cli.getPassword();
        final boolean passwordPresent = password.isPresent();
        final boolean useToken = cli.isTokenEnabled();
        if (!username.isPresent()) {
            App.printHelpAndExit(cli, "Username must be provided.", true);
        }
        final String usr = username.get();
        if (!useToken) { // using password-based authentication
            if (!passwordPresent) {
                App.printHelpAndExit(cli, "Not using refresh token, password must be provided.", true);
            }
            return AuthenticationMethod.withCredentials(usr, password.get());
        }
        final String pwd = password.get();
        if (!App.TOKEN_FILE.canRead()) { // no token available, also using password-based
            App.LOGGER.debug("Token file not available for reading, using password-based authentication.");
            return AuthenticationMethod.withCredentials(usr, pwd);
        }
        boolean deleteToken = false;
        try {
            final ZonkyApiToken token = ZonkyApiToken.unmarshal(App.TOKEN_FILE);
            final LocalDateTime obtained = LocalDateTime.ofInstant(Instant.ofEpochMilli(App.TOKEN_FILE.lastModified()),
                    ZoneId.systemDefault());
            final LocalDateTime expires = obtained.plus(token.getExpiresIn(), ChronoUnit.SECONDS);
            App.LOGGER.debug("Token obtained on {}, expires on {}.", obtained, expires);
            final LocalDateTime now = LocalDateTime.now();
            if (expires.isBefore(now)) {
                App.LOGGER.debug("Token {} expired, using password-based authentication.", token.getAccessToken());
                deleteToken = true;
                return AuthenticationMethod.withCredentials(usr, pwd);
            } else if (expires.minus(App.REFRESH_TOKEN_BEFORE_EXPIRATION_SECONDS, ChronoUnit.SECONDS).isBefore(now)) {
                App.LOGGER.debug("Token {} about to expire and will be refreshed.", token.getAccessToken());
                deleteToken = true;
                return AuthenticationMethod.withAccessTokenAndRefresh(usr, token);
            } else {
                App.LOGGER.debug("Reusing access token {}.", token.getAccessToken());
                return AuthenticationMethod.withAccessToken(usr, token);
            }
        } catch (final JAXBException ex) {
            App.LOGGER.warn("Failed parsing token file, using password-based authentication.", ex);
            deleteToken = true;
            return AuthenticationMethod.withCredentials(usr, pwd);
        } finally {
            if (deleteToken && !App.TOKEN_FILE.delete()) {
                App.LOGGER.warn("Failed deleting token file, authentication may stop working.");
            }
        }
    }

    private static AppContext prepareUserDrivenMode(final CommandLineInterface cli) {
        final Optional<Integer> loanId = cli.getLoanId();
        final Optional<Integer> loanAmount = cli.getLoanAmount();
        if (!loanId.isPresent()) {
            App.printHelpAndExit(cli, "Loan ID must be provided.", true);
        } else if (!loanAmount.isPresent()) {
            App.printHelpAndExit(cli, "Loan amount must be provided.", true);
        }
        final AuthenticationMethod auth = App.getAuthenticationMethod(cli);
        if (cli.isDryRun()) {
            return new AppContext(auth, cli.isTokenEnabled(), loanId.get(), loanAmount.get(), cli.getDryRunBalance());
        } else {
            return new AppContext(auth, cli.isTokenEnabled(), loanId.get(), loanAmount.get());
        }
    }

    static AppContext processCommandLine(final String... args) {
        final CommandLineInterface cmd = CommandLineInterface.parse(args);
        switch (cmd.getCliOperatingMode()) {
            case HELP:
                App.printHelpAndExit(cmd, "", false);
                return null; // just in case exit control property is tests
            case STRATEGY_DRIVEN:
                return App.prepareStrategyDrivenMode(cmd);
            case USER_DRIVER:
                return App.prepareUserDrivenMode(cmd);
        }
        throw new IllegalStateException("This should not have happened.");
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

    static void letsGo(final AppContext ctx) {
        App.LOGGER.info("===== RoboZonky at your service! =====");
        final boolean dryRun = ctx.isDryRun();
        if (dryRun) {
            App.LOGGER.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        // figure out whether to invest based on strategy or whether to make a single investment
        final boolean useStrategy = ctx.getOperatingMode() == OperatingMode.STRATEGY_DRIVEN;
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
        final boolean useStrategy = ctx.getOperatingMode() == OperatingMode.STRATEGY_DRIVEN;
        try {
            final OperationsContext oc = useStrategy ?
                    Operations.login(ctx.getAuthenticationMethod(), ctx.isDryRun(), ctx.getDryRunBalance(),
                            ctx.getInvestmentStrategy()) :
                    Operations.login(ctx.getAuthenticationMethod(), ctx.isDryRun(), ctx.getDryRunBalance());
            boolean useToken = ctx.isTokenUsed();
            if (useToken && !App.TOKEN_FILE.exists()) {
                final ZonkyApiToken t = oc.getApiToken();
                try {
                    ZonkyApiToken.marshal(t, App.TOKEN_FILE);
                } catch (final JAXBException ex) {
                    App.LOGGER.info("Failed writing access token, will need to use password next time.", ex);
                    useToken = false;
                }
            }
            final Collection<Investment> result = operations.apply(oc);
            if (useToken) { // if we're using the token, we should never log out
                App.LOGGER.info("Refresh token stored, not logging out of Zonky.");
            } else { // log out
                try {
                    Operations.logout(oc);
                } catch (final LogoutFailedException ex) {
                    App.LOGGER.warn("Logging out of Zonky failed.", ex);
                }
            }
            return result;
        } catch (final LoginFailedException ex) {
            App.LOGGER.error("Logging into Zonky failed. No investments were made.", ex);
            App.exit(ReturnCode.ERROR_LOGIN);
        }
        return Collections.emptyList(); // should never get here
    }

}
