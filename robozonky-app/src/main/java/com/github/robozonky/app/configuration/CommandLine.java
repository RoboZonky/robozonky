/*
 * Copyright 2019 The RoboZonky Project
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import com.github.robozonky.app.App;
import com.github.robozonky.app.runtime.Lifecycle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Converts command line into application configuration using {@link picocli.CommandLine}.
 */
@picocli.CommandLine.Command(name = "robozonky(.sh|.bat)")
public class CommandLine implements Callable<Optional<InvestmentMode>> {

    private static final Logger LOGGER = LogManager.getLogger(CommandLine.class);
    private final Supplier<Lifecycle> lifecycle;
    @picocli.CommandLine.Option(names = {"-s", "--strategy"}, required = true,
            description = "Points to a resource holding the investment strategy configuration.")
    String strategyLocation = "";
    @picocli.CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Print usage end exit.")
    private boolean help;
    @picocli.CommandLine.Option(names = {"-i", "--inform"},
            description = "Configure RoboZonky notifications from a given location.")
    private String notificationConfigLocation;
    @picocli.CommandLine.Option(names = {"-n", "--name"}, description = "Name of this RoboZonky session.")
    private String name = "Unnamed";
    @picocli.CommandLine.Option(names = {"-p", "--password"}, required = true, interactive = true,
            arity = "0..1", description = "Enter Zonky account password or secure storage password.")
    private char[] password = null;
    @picocli.CommandLine.Option(names = {"-d", "--dry"},
            description = "RoboZonky will simulate investments, but never actually spend money.")
    private boolean dryRunEnabled = false;
    @picocli.CommandLine.Option(names = {"-g", "--guarded"},
            description = "Path to secure file that contains username, password etc.", required = true)
    private File keystore = null;

    public CommandLine(final Supplier<Lifecycle> lifecycle) {
        this.lifecycle = lifecycle;
        // for backwards compatibility with RoboZonky 4.x, which used JCommander
        System.setProperty("picocli.trimQuotes", "true");
        System.setProperty("picocli.useSimplifiedAtFiles", "true");
    }

    /**
     * Takes command-line arguments and converts them into an application configuration, printing command line usage
     * information in case the arguments are somehow invalid.
     * @param main The code that called this code.
     * @return Present if the arguments resulted in a valid configuration, empty otherwise.
     */
    public static Optional<InvestmentMode> parse(final App main) {
        // parse the arguments
        final CommandLine cli = new CommandLine(main::getLifecycle);
        final picocli.CommandLine pico = new picocli.CommandLine(cli);
        pico.execute(main.getArgs());
        final Optional<InvestmentMode> result = pico.getExecutionResult();
        return Objects.isNull(result) ? Optional.empty() : result;
    }

    boolean isDryRunEnabled() {
        return dryRunEnabled;
    }

    String getStrategyLocation() {
        return strategyLocation;
    }

    Optional<URL> getNotificationConfigLocation() {
        if (notificationConfigLocation == null) {
            LOGGER.info("Notifications are not set up.");
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
                throw new IllegalStateException("Incorrect format for notification configuration location.", ex2);
            }
        }
    }

    char[] getPassword() {
        return password;
    }

    Optional<File> getKeystore() {
        return Optional.ofNullable(keystore);
    }

    String getName() {
        return name;
    }

    @Override
    public Optional<InvestmentMode> call() {
        final OperatingMode mode = new OperatingMode(lifecycle);
        return SecretProviderFactory.getSecretProvider(this)
                .map(secrets -> mode.configure(this, secrets));
    }
}
