/*
 * Copyright 2018 The RoboZonky Project
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.github.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CommandLine {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLine.class);
    @Parameter(names = {"-h", "--help"}, help = true, description = "Print usage end exit.")
    private boolean help;

    public static Optional<Feature> parse(final String... args) {
        try {
            return CommandLine.parseUnsafe(args);
        } catch (final ParameterException ex) {
            CommandLine.LOGGER.warn("Command line parsing failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    static String getProgramName() {
        return "java -jar robozonky-distribution-cli-" + Defaults.ROBOZONKY_VERSION + "-full.jar";
    }

    static Stream<Feature> listFeatures() {
        return Stream.of(new ZonkyPasswordFeature(), new ZonkoidPasswordFeature(), new MasterPasswordFeature(),
                         new StrategyValidationFeature(), new NotificationTestingFeature());
    }

    private static Optional<Feature> parseUnsafe(final String... args) {
        final CommandLine cli = new CommandLine();
        final JCommander.Builder builder = new JCommander.Builder()
                .programName(getProgramName())
                .addObject(cli);
        listFeatures().forEach(builder::addCommand);
        final JCommander jc = builder.build();
        jc.parse(args);
        if (cli.help) { // don't validate since the CLI is likely to be invalid
            jc.usage();
            return Optional.of(new HelpFeature());
        }
        return Optional.of(findFeature(jc));
    }

    private static Feature findFeature(final JCommander jc) {
        final String parsedCommand = jc.getParsedCommand();
        if (parsedCommand == null) {
            throw new ParameterException("You must specify a command. See usage.");
        }
        final JCommander command = jc.getCommands().get(parsedCommand);
        final List<Object> objects = command.getObjects();
        return (Feature) objects.get(0);
    }
}
