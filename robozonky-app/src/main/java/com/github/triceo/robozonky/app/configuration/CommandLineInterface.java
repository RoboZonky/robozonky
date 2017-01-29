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

import java.util.Optional;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.investing.InvestmentMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts command line into application configuration using {@link JCommander}.
 */
public class CommandLineInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineInterface.class);

    private static Optional<InvestmentMode> terminate(final JCommander jc, final String message) {
        System.out.println(message);
        return CommandLineInterface.terminate(jc);
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
     *
     * @param args Command-line arguments, coming from the main() method.
     * @return Present if the arguments resulted in a valid configuration, empty otherwise.
     */
    public static Optional<InvestmentMode> parse(final String... args) {
        final CommandLineInterface cli = new CommandLineInterface();
        final JCommander jc = new JCommander(cli);
        jc.setProgramName(CommandLineInterface.getScriptIdentifier());
        Stream.of(OperatingMode.values()).forEach(mode -> jc.addCommand(mode.getName(), mode));
        try { // internal validation
            jc.parse(args);
            if (cli.help) { // don't validate since the CLI is likely to be invalid
                return CommandLineInterface.terminate(jc);
            }
            cli.setImplementation(jc);
            cli.validate();
            return cli.newApplicationConfiguration();
        } catch (final ParameterException ex) {
            CommandLineInterface.LOGGER.debug("Command line parsing ended with parameter exception.", ex);
            return CommandLineInterface.terminate(jc, ex.getMessage());
        }
    }

    private OperatingMode mode;

    private void validate() throws ParameterException {
        Stream.of(authenticationFragment, mode, confirmationFragment, tweaksFragment).forEach(CommandLineFragment::validate);
    }

    private void setImplementation(final JCommander jc) {
        this.mode = (OperatingMode)jc.getCommands().get(jc.getParsedCommand()).getObjects().get(0);
    }

    private Optional<InvestmentMode> newApplicationConfiguration() {
        final Optional<AuthenticationHandler> authenticationHandler = SecretProviderFactory.getSecretProvider(this)
                .map(secrets -> Optional.of(authenticationFragment.createAuthenticationHandler(secrets)))
                .orElse(Optional.empty());
        return authenticationHandler
                .map(auth -> Optional.ofNullable(mode)
                    .map(i -> i.configure(this, auth))
                    .orElse(Optional.empty()))
                .orElse(Optional.empty());
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
