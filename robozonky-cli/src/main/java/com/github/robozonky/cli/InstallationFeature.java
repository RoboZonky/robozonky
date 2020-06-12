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

package com.github.robozonky.cli;

import static picocli.CommandLine.Option;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.robozonky.cli.configuration.ConfigurationModel;
import com.github.robozonky.cli.configuration.NotificationConfiguration;
import com.github.robozonky.cli.configuration.PropertyConfiguration;
import com.github.robozonky.cli.configuration.StrategyConfiguration;

import picocli.CommandLine;

@CommandLine.Command(name = "install", description = InstallationFeature.DESCRIPTION)
public final class InstallationFeature extends AbstractFeature {

    static final String DESCRIPTION = "Create the RoboZonky installation directory as if through the installer.";

    @Option(names = { "-r",
            "--robozonky" }, description = "Full path to the directory containing unzipped RoboZonky distribution.", required = true)
    Path distribution;
    @Option(names = { "-i",
            "--install-dir" }, description = "Full path to the directory in which the installation files will be copied.", required = true)
    Path installation = Paths.get(System.getProperty("user.dir"));
    @Option(names = { "-k", "--keystore" }, description = "The keystore to hold the secrets.", required = true)
    Path keystore;
    @Option(names = { "-p",
            "--password" }, description = "Secret to use to access the keystore.", required = true, interactive = true, arity = "0..1")
    char[] secret = null;
    @Option(names = { "-u", "--username" }, description = "Zonky username.", required = true)
    String username = null;
    @Option(names = { "-n", "--notifications" }, description = "URL leading to notification configuration file.")
    URL notificationLocation;
    @Option(names = { "-s", "--strategy" }, description = "URL leading to the strategy file.", required = true)
    URL strategyLocation;
    @Option(names = { "-j",
            "--jmx-hostname" }, description = "Enables JMX, will listen at this hostname and/or address.")
    URL jmxHostname;
    @Option(names = { "-x", "--jmx-port" }, description = "Port to expose JMX on, if enabled.")
    int jmxPort = 7091;
    @Option(names = { "-d", "--dry-run" }, description = "Whether to run RoboZonky in dry run.")
    boolean dryRunEnabled = false;
    @Option(names = { "-w",
            "--windows" }, description = "If provided, generate files for Windows. Otherwise Unix files will be generated.")
    boolean windows = false;

    @Override
    public String describe() {
        return DESCRIPTION;
    }

    @Override
    public void setup() throws SetupFailedException {
        try {
            PropertyConfiguration application = dryRunEnabled
                    ? PropertyConfiguration.applicationDryRun(keystore, secret)
                    : PropertyConfiguration.applicationReal(keystore, secret);
            PropertyConfiguration jmx = jmxHostname == null ? PropertyConfiguration.disabledJmx()
                    : PropertyConfiguration.enabledJmx(jmxHostname.toExternalForm(), jmxPort, false);
            StrategyConfiguration strategy = StrategyConfiguration.remote(strategyLocation.toExternalForm());
            NotificationConfiguration notifications = NotificationConfiguration
                .remote(notificationLocation.toExternalForm());
            ConfigurationModel configurationModel = ConfigurationModel.load(application, strategy, notifications, jmx);
            configurationModel.materialize(distribution, installation, !windows);
        } catch (Exception ex) {
            throw new SetupFailedException("Installation failed.", ex);
        }
    }

    @Override
    public void test() {
        // There is no test for this action.
    }

}
