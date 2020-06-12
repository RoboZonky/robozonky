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

package com.github.robozonky.cli.configuration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.cli.configuration.scripts.RunScriptGenerator;
import com.github.robozonky.cli.configuration.scripts.ServiceGenerator;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.Settings;

public final class ConfigurationModel {

    private static final Logger LOGGER = LogManager.getLogger(ConfigurationModel.class);

    private final ApplicationConfiguration applicationConfiguration;
    private final StrategyConfiguration strategyConfiguration;
    private final NotificationConfiguration notificationConfiguration;
    private final PropertyConfiguration jmxConfiguration;

    private ConfigurationModel(final ApplicationConfiguration applicationConfiguration,
            final StrategyConfiguration strategyConfiguration,
            final NotificationConfiguration notificationConfiguration,
            final PropertyConfiguration jmxConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
        this.strategyConfiguration = strategyConfiguration;
        this.notificationConfiguration = notificationConfiguration;
        this.jmxConfiguration = jmxConfiguration;
    }

    public static ConfigurationModel load(final PropertyConfiguration applicationConfiguration,
            final StrategyConfiguration strategyConfiguration,
            final NotificationConfiguration notificationConfiguration, final PropertyConfiguration jmxConfiguration) {
        return new ConfigurationModel((ApplicationConfiguration) applicationConfiguration, strategyConfiguration,
                notificationConfiguration, jmxConfiguration);
    }

    private static void prepareLinuxServices(final File runScript) {
        for (final ServiceGenerator serviceGenerator : ServiceGenerator.values()) {
            final File result = serviceGenerator.apply(runScript);
            LOGGER.info("Generated {} as a {} service.", result, serviceGenerator);
        }
    }

    public void materialize(Path distributionRoot, Path installationRoot, boolean unix) {
        String lineSeparator = unix ? "\n" : "\r\n";
        // Prime all configurations.
        Stream.of(applicationConfiguration, strategyConfiguration, notificationConfiguration, jmxConfiguration)
            .forEach(config -> {
                try {
                    config.accept(distributionRoot, installationRoot);
                } catch (final Exception ex) {
                    throw new IllegalStateException("Installation failed in " + config, ex);
                }
            });
        Path robozonkyCli = installationRoot.resolve("robozonky.cli");
        try { // Assemble robozonky.cli.
            List<String> cliLines = new ArrayList<>(0);
            applicationConfiguration.getApplicationArguments()
                .forEach((k, v) -> {
                    cliLines.add("-" + k);
                    if (!v.isBlank()) {
                        cliLines.add("\"" + v + "\"");
                    }
                });
            cliLines.add("-s");
            cliLines.add("\"" + strategyConfiguration.getFinalLocation() + "\"");
            notificationConfiguration.getFinalLocation()
                .ifPresent(n -> {
                    cliLines.add("-i");
                    cliLines.add("\"" + n + "\"");
                });
            String robozonkyCliContents = String.join(lineSeparator, cliLines);
            Files.write(robozonkyCli, robozonkyCliContents.getBytes(Defaults.CHARSET));
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed preparing command-line.", ex);
        }
        List<String> jvmArguments = new ArrayList<>();
        applicationConfiguration.getJvmArguments()
            .forEach((k, v) -> {
                if (v.isBlank()) {
                    jvmArguments.add("-" + k);
                } else {
                    jvmArguments.add("-" + k + "=" + v);
                }
            });
        jvmArguments.add("-D" + Settings.FILE_LOCATION_PROPERTY + "=\""
                + installationRoot.resolve("robozonky.properties") + "\"");
        try { // Assemble robozonky.properties
            Properties robozonkyProperties = new Properties();
            Stream.of(applicationConfiguration, strategyConfiguration, notificationConfiguration, jmxConfiguration)
                .filter(c -> c instanceof PropertyConfiguration)
                .map(c -> (PropertyConfiguration) c)
                .forEach(c -> c.getProperties()
                    .forEach((k, v) -> {
                        if (k.startsWith("robozonky.")) {
                            robozonkyProperties.setProperty(k, v);
                        } else {
                            jvmArguments.add("-D" + k + "=\"" + v + "\"");
                        }
                    }));
            Path target = installationRoot.resolve("robozonky.properties");
            Util.writeOutProperties(robozonkyProperties, target);
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed preparing properties.", ex);
        }
        final RunScriptGenerator generator = unix
                ? RunScriptGenerator.forUnix(distributionRoot.toFile(), robozonkyCli.toFile())
                : RunScriptGenerator.forWindows(distributionRoot.toFile(), robozonkyCli.toFile());
        final File runScript = generator.apply(jvmArguments);
        final File distRunScript = generator.getChildRunScript();
        Stream<File> toMakeExecutable = Stream.of(runScript, distRunScript);
        final File javaExecutable = distributionRoot.resolve(unix ? "jre/bin/java" : "jre/bin/java.exe")
            .toFile();
        if (javaExecutable.exists()) {
            toMakeExecutable = Stream.concat(Stream.of(javaExecutable), toMakeExecutable);
        } else {
            LOGGER.info("Bundled Java binary not found, not making it executable.");
        }
        toMakeExecutable.forEach(file -> {
            final boolean success = file.setExecutable(true);
            LOGGER.info("Made '{}' executable: {}.", file, success);
        });
        if (unix) {
            prepareLinuxServices(runScript);
        }
    }

}
