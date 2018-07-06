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

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandLineTest {

    @Test
    void zonkyPassword() {
        final Optional<Feature> f =
                CommandLine.parse("zonky-credentials", "-k", "file", "-s", "secret", "-u", "user", "-p", "pass");
        assertThat(f).containsInstanceOf(ZonkyPasswordFeature.class);
    }

    @Test
    void zonkoidPassword() {
        final Optional<Feature> f =
                CommandLine.parse("zonkoid-credentials", "-k", "file", "-s", "secret", "-p", "pass");
        assertThat(f).containsInstanceOf(ZonkoidPasswordFeature.class);
    }

    @Test
    void masterPassword() {
        final Optional<Feature> f =
                CommandLine.parse("master-password", "-k", "file", "-s", "secret", "-n", "pass");
        assertThat(f).containsInstanceOf(MasterPasswordFeature.class);
    }

    @Test
    void strategyValidation() {
        final Optional<Feature> f = CommandLine.parse("strategy-validator", "-l", "http://location");
        assertThat(f).containsInstanceOf(StrategyValidationFeature.class);
    }

    @Test
    void notificationsTesting() {
        final Optional<Feature> f = CommandLine.parse("notification-tester", "-u", "user", "-l", "http://location");
        assertThat(f).containsInstanceOf(NotificationTestingFeature.class);
    }

    @Test
    void help() {
        final Optional<Feature> f = CommandLine.parse("-h");
        assertThat(f).containsInstanceOf(HelpFeature.class);
    }

    @Test
    void wrong() {
        final Optional<Feature> f = CommandLine.parse();
        assertThat(f).isEmpty();
    }

    @Test
    void programName() {
        assertThat(CommandLine.getProgramName()).isNotNull();
    }
}
