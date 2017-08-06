/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.configuration;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts command line into application configuration using {@link JCommander}.
 */
public class CommandLine {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLine.class);

    private static Optional<InvestmentMode> terminate(final ParameterException ex) {
        System.out.println(ex.getMessage()); // error will be shown to users on stdout
        return CommandLine.terminate(ex.getJCommander());
    }

    private static Optional<InvestmentMode> terminate(final JCommander jc) {
        jc.usage();
        return Optional.empty();
    }

    @ParametersDelegate
    private AuthenticationCommandLineFragment authenticationFragment = new AuthenticationCommandLineFragment();

    @ParametersDelegate
    private TweaksCommandLineFragment tweaksFragment = new TweaksCommandLineFragment();

    @ParametersDelegate
    private ConfirmationCommandLineFragment confirmationFragment = new ConfirmationCommandLineFragment();

    @Parameter(names = {"-h", "--help"}, help = true, description = "Print usage end exit.")
    private boolean help;

    /**
     * Takes command-line arguments and converts them into an application configuration, printing command line usage
     * information in case the arguments are somehow invalid.
     * @param args Command-line arguments, coming from the main() method.
     * @return Present if the arguments resulted in a valid configuration, empty otherwise.
     */
    public static Optional<InvestmentMode> parse(final String... args) {
        try {
            return CommandLine.parseUnsafe(args);
        } catch (final ParameterException ex) {
            CommandLine.LOGGER.debug("Command line parsing ended with parameter exception.", ex);
            return CommandLine.terminate(ex);
        }
    }

    private static Optional<InvestmentMode> parseUnsafe(final String... args) throws ParameterException {
        final CommandLine cli = new CommandLine();
        final JCommander.Builder builder = new JCommander.Builder()
                .programName(CommandLine.getScriptIdentifier())
                .addCommand(new DaemonOperatingMode())
                .addCommand(new TestOperatingMode())
                .addObject(cli);
        final JCommander jc = builder.build();
        jc.parse(args);
        if (cli.help) { // don't validate since the CLI is likely to be invalid
            return CommandLine.terminate(jc);
        }
        final OperatingMode mode = cli.determineOperatingMode(jc);
        return cli.newApplicationConfiguration(mode);
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
        return SecretProviderFactory.getSecretProvider(this).flatMap(secrets -> Optional.ofNullable(mode).flatMap(
                i -> i.configure(this, authenticationFragment.createAuthenticated(secrets))));
    }

    AuthenticationCommandLineFragment getAuthenticationFragment() {
        return authenticationFragment;
    }

    TweaksCommandLineFragment getTweaksFragment() {
        return tweaksFragment;
    }

    ConfirmationCommandLineFragment getConfirmationFragment() {
        return confirmationFragment;
    }

    static String getScriptIdentifier() {
        return System.getProperty("os.name").contains("Windows") ? "robozonky.bat" : "robozonky.sh";
    }
}
