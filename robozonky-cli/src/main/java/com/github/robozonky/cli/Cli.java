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

import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(name = "java -jar robozonky-cli.jar", subcommands = {
        CommandLine.HelpCommand.class,
        MasterPasswordFeature.class,
        NotificationTestingFeature.class,
        StrategyValidationFeature.class,
        ZonkyCredentialsFeature.class
})
final class Cli implements Callable<Integer> {

    public static int parse(final String... args) {
        final CommandLine cli = new CommandLine(new Cli());
        return cli.execute(args);
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.err);
        return CommandLine.ExitCode.USAGE;
    }
}
