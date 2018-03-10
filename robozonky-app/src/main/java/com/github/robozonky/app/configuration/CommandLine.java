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

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;
import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.app.App;
import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.internal.api.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts command line into application configuration using {@link JCommander}.
 */
public class CommandLine {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLine.class);
    @ParametersDelegate
    private AuthenticationCommandLineFragment authenticationFragment = new AuthenticationCommandLineFragment();
    @ParametersDelegate
    private TweaksCommandLineFragment tweaksFragment = new TweaksCommandLineFragment();
    @ParametersDelegate
    private ConfirmationCommandLineFragment confirmationFragment = new ConfirmationCommandLineFragment();
    @Parameter(names = {"-h", "--help"}, help = true, description = "Print usage end exit.")
    private boolean help;
    @Parameter(names = {"-n", "--name"}, description = "Name of this RoboZonky session.")
    private String name;

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
        final CommandLine cli = new CommandLine();
        final JCommander.Builder builder = new JCommander.Builder()
                .programName(CommandLine.getScriptIdentifier())
                .addCommand(new DaemonOperatingMode(shutdownCall))
                .addCommand(new TestOperatingMode())
                .addObject(cli);
        final JCommander jc = builder.build();
        jc.parse(args);
        if (cli.help) { // don't validate since the CLI is likely to be invalid
            CommandLine.terminate(jc);
            return Optional.empty();
        }
        final OperatingMode mode = cli.determineOperatingMode(jc);
        return cli.newApplicationConfiguration(mode);
    }

    static String getScriptIdentifier() {
        return System.getProperty("os.name").contains("Windows") ? "robozonky.bat" : "robozonky.sh";
    }

    private OperatingMode determineOperatingMode(final JCommander jc) throws ParameterException {
        final String parsedCommand = jc.getParsedCommand();
        if (parsedCommand == null) {
            final ParameterException ex = new ParameterException("You must specify one mode of operation. See usage.");
            ex.setJCommander(jc);
            throw ex;
        }
        final JCommander command = jc.getCommands().get(parsedCommand);
        final List<Object> objects = command.getObjects();
        final OperatingMode mode = (OperatingMode) objects.get(0);
        Stream.of(authenticationFragment, confirmationFragment, tweaksFragment, mode)
                .forEach(commandLineFragment -> commandLineFragment.validate(jc));
        return mode;
    }

    private Optional<InvestmentMode> newApplicationConfiguration(final OperatingMode mode) {
        return SecretProviderFactory.getSecretProvider(authenticationFragment)
                .flatMap(secrets -> {
                    final Duration duration = Settings.INSTANCE.getTokenRefreshPeriod();
                    final Authenticated auth = Authenticated.tokenBased(secrets, duration);
                    return mode.configure(this, auth);
                });
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
