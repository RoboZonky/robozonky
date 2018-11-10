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
import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(name = "java -jar robozonky-cli.jar",
        subcommands = {
                CommandLine.HelpCommand.class,
                GoogleCredentialsFeature.class,
                MasterPasswordFeature.class,
                NotificationTestingFeature.class,
                StrategyValidationFeature.class,
                ZonkoidPasswordFeature.class,
                ZonkyPasswordFeature.class
        })
final class Cli implements Callable<ExitCode> {

    public static Optional<ExitCode> parse(final String... args) {
        final CommandLine cli = new CommandLine(new Cli());
        final List<?> o = cli.parseWithHandlers(new CommandLine.RunLast().useAnsi(CommandLine.Help.Ansi.ON),
                                                CommandLine.defaultExceptionHandler()
                                                        .useAnsi(CommandLine.Help.Ansi.OFF),
                                                args);
        if (o == null) {
            return Optional.of(ExitCode.NO_OPERATION);
        } else {
            return Optional.ofNullable((ExitCode) o.get(0));
        }
    }

    @Override
    public ExitCode call() {
        CommandLine.usage(this, System.err);
        return ExitCode.NO_OPERATION;
    }
}
