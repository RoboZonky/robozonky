/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.app;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.app.daemon.Daemon;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.app.events.SessionEvents;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.app.tenant.TenantBuilder;
import com.github.robozonky.internal.extensions.ListenerServiceLoader;
import com.github.robozonky.internal.secrets.SecretProvider;
import com.github.robozonky.internal.util.UrlUtil;

/**
 * Converts command line into application configuration using {@link picocli.CommandLine}.
 */
@Command(name = "robozonky(.sh|.bat)")
public class CommandLine implements Callable<Optional<Function<Lifecycle, InvestmentMode>>> {

    private static final Logger LOGGER = LogManager.getLogger(CommandLine.class);
    @Option(names = { "-s",
            "--strategy" }, required = true, description = "Points to a resource holding the investment strategy configuration.")
    String strategyLocation = "";
    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Print usage end exit.")
    private boolean help;
    @Option(names = { "-i", "--inform" }, description = "Points to a resource holding the notification configuration.")
    private String notificationConfigLocation;
    @Option(names = { "-n", "--name" }, description = "Name of this RoboZonky session.")
    private String name = "Unnamed";
    @Option(names = { "-p",
            "--password" }, required = true, interactive = true, arity = "0..1", description = "Enter password for the secure storage file.")
    private char[] password = null;
    @Option(names = { "-d",
            "--dry" }, description = "RoboZonky will simulate investments, but never actually spend money.")
    private boolean dryRunEnabled = false;
    @Option(names = { "-g",
            "--guarded" }, description = "Path to secure storage file that contains username, password etc.", required = true)
    private File keystore = null;

    public CommandLine() {
        // for backwards compatibility with RoboZonky 4.x, which used JCommander
        System.setProperty("picocli.trimQuotes", "true");
        System.setProperty("picocli.useSimplifiedAtFiles", "true");
    }

    /**
     * Takes command-line arguments and converts them into an application configuration, printing command line usage
     * information in case the arguments are somehow invalid.
     * 
     * @param main The code that called this code.
     * @return Present if the arguments resulted in a valid configuration, empty otherwise.
     */
    public static Optional<Function<Lifecycle, InvestmentMode>> parse(final App main) {
        // parse the arguments
        final CommandLine cli = new CommandLine();
        final picocli.CommandLine pico = new picocli.CommandLine(cli);
        pico.execute(main.getArgs());
        final Optional<Function<Lifecycle, InvestmentMode>> result = pico.getExecutionResult();
        return Objects.isNull(result) ? Optional.empty() : result;
    }

    private static PowerTenant getTenant(final CommandLine cli, final SecretProvider secrets) {
        final TenantBuilder b = new TenantBuilder();
        if (cli.dryRunEnabled) {
            LOGGER.info("RoboZonky is doing a dry run. It will not invest any real money.");
            b.dryRun();
        }
        return b.withSecrets(secrets)
            .withStrategy(cli.strategyLocation)
            .named(cli.name)
            .build();
    }

    private static void configureNotifications(final CommandLine cli, final PowerTenant tenant) {
        // unregister if registered
        final SessionInfo session = tenant.getSessionInfo();
        ListenerServiceLoader.unregisterConfiguration(session);
        // register if needed
        cli.getNotificationConfigLocation()
            .ifPresent(cfg -> ListenerServiceLoader.registerConfiguration(session, cfg));
        // create event handler for this session, otherwise session-less notifications will not be sent
        final SessionEvents e = Events.forSession(tenant);
        LOGGER.debug("Notification subsystem initialized: {}.", e);
    }

    Optional<URL> getNotificationConfigLocation() {
        if (notificationConfigLocation == null) {
            LOGGER.info("Notifications are not set up.");
            return Optional.empty();
        }
        return Optional.of(UrlUtil.toURL(notificationConfigLocation));
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

    private InvestmentMode configure(final SecretProvider secrets, final Lifecycle lifecycle) {
        final PowerTenant tenant = getTenant(this, secrets);
        configureNotifications(this, tenant);
        // and now initialize the chosen mode of operation
        return new Daemon(tenant, lifecycle);
    }

    @Override
    public Optional<Function<Lifecycle, InvestmentMode>> call() {
        return SecretProviderFactory.getSecretProvider(this)
            .map(s -> l -> configure(s, l));
    }
}
