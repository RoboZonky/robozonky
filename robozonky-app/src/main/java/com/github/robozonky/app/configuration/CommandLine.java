/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.configuration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Consumer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;
import com.github.robozonky.app.App;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.ShutdownHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts command line into application configuration using {@link JCommander}.
 */
public class CommandLine {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLine.class);
    private final Consumer<Throwable> shutdownCall;
    @Parameter(names = {"-s", "--strategy"}, required = true,
            description = "Points to a resource holding the investment strategy configuration.")
    String strategyLocation = "";
    @ParametersDelegate
    private MarketplaceCommandLineFragment marketplace = new MarketplaceCommandLineFragment();
    @ParametersDelegate
    private AuthenticationCommandLineFragment authenticationFragment = new AuthenticationCommandLineFragment();
    @ParametersDelegate
    private TweaksCommandLineFragment tweaksFragment = new TweaksCommandLineFragment();
    @ParametersDelegate
    private ConfirmationCommandLineFragment confirmationFragment = new ConfirmationCommandLineFragment();
    @Parameter(names = {"-h", "--help"}, help = true, description = "Print usage end exit.")
    private boolean help;
    @Parameter(names = {"-i", "--inform"}, description = "Configure RoboZonky notifications from a given location.")
    private String notificationConfigLocation;
    @Parameter(names = {"-n", "--name"}, description = "Name of this RoboZonky session.")
    private String name;

    public CommandLine(final Consumer<Throwable> shutdownCall) {
        this.shutdownCall = shutdownCall;
    }

    private static void terminate(final ParameterException ex) {
        System.out.println(ex.getMessage()); // error will be shown to users on stdout
        ex.getJCommander().usage();
        App.exit(new ShutdownHook.Result(ReturnCode.ERROR_WRONG_PARAMETERS, null));
    }

    private static void terminate(final JCommander jc) {
        jc.usage();
        App.exit(new ShutdownHook.Result(ReturnCode.OK, null));
    }

    /**
     * Takes command-line arguments and converts them into an application configuration, printing command line usage
     * information in case the arguments are somehow invalid.
     * @param args Command-line arguments, coming from the main() method.
     * @param shutdownCall To pass to the daemon operations to allow them to shut down properly.
     * @return Present if the arguments resulted in a valid configuration, empty otherwise.
     */
    public static Optional<InvestmentMode> parse(final Consumer<Throwable> shutdownCall, final String... args) {
        try {
            return CommandLine.parseUnsafe(shutdownCall, args);
        } catch (final ParameterException ex) {
            CommandLine.LOGGER.debug("Command line parsing ended with parameter exception.", ex);
            CommandLine.terminate(ex);
            return Optional.empty();
        }
    }

    private static Optional<InvestmentMode> parseUnsafe(final Consumer<Throwable> shutdownCall,
                                                        final String... args) throws ParameterException {
        final CommandLine cli = new CommandLine(shutdownCall);
        final JCommander.Builder builder = new JCommander.Builder()
                .programName(CommandLine.getScriptIdentifier())
                .addObject(cli);
        final JCommander jc = builder.build();
        jc.parse(args);
        if (cli.help) { // don't validate since the CLI is likely to be invalid
            CommandLine.terminate(jc);
            return Optional.empty();
        }
        return cli.newApplicationConfiguration();
    }

    static String getScriptIdentifier() {
        return System.getProperty("os.name").contains("Windows") ? "robozonky.bat" : "robozonky.sh";
    }

    MarketplaceCommandLineFragment getMarketplace() {
        return marketplace;
    }

    String getStrategyLocation() {
        return strategyLocation;
    }

    private Optional<InvestmentMode> newApplicationConfiguration() {
        final OperatingMode mode = new OperatingMode(shutdownCall);
        return SecretProviderFactory.getSecretProvider(authenticationFragment)
                .flatMap(secrets -> mode.configure(this, secrets));
    }

    Optional<URL> getNotificationConfigLocation() {
        if (notificationConfigLocation == null) {
            LOGGER.debug("No notification configuration for tenant.");
            return Optional.empty();
        }
        try {
            final URL url = new URL(notificationConfigLocation);
            return Optional.of(url);
        } catch (final MalformedURLException ex) {
            final File f = new File(notificationConfigLocation);
            try {
                return Optional.of(f.getAbsoluteFile().toURI().toURL());
            } catch (final MalformedURLException ex2) {
                throw new ParameterException("Incorrect format for notification configuration location.");
            }
        }
    }

    String getName() {
        return name;
    }

    TweaksCommandLineFragment getTweaksFragment() {
        return tweaksFragment;
    }

    ConfirmationCommandLineFragment getConfirmationFragment() {
        return confirmationFragment;
    }
}
