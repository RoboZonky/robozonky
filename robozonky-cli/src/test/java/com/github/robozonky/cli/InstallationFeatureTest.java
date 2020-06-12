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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class InstallationFeatureTest {

    @Test
    void setupWindowsDry() throws IOException, SetupFailedException {
        // Setup stuff.
        InstallationFeature feature = new InstallationFeature();
        feature.windows = true;
        feature.dryRunEnabled = true;
        feature.jmxHostname = new URL("http://localhost");
        feature.distribution = Files.createTempDirectory("robozonky-distribution");
        feature.installation = Files.createTempDirectory("robozonky-install");
        feature.keystore = Files.createTempFile("robozonky-", ".keystore");
        feature.secret = UUID.randomUUID()
            .toString()
            .toCharArray();
        feature.strategyLocation = new URL("http://somewhere.props");
        feature.notificationLocation = new URL("http://somewhere.cfg");
        feature.username = "someone@somewhere.cz";
        // Run the test.
        feature.setup();
        feature.test();
        // Check output.
        assertSoftly(softly -> {
            softly.assertThat(feature.installation.resolve("robozonky.properties"))
                .exists();
            softly.assertThat(feature.installation.resolve("management.properties"))
                .exists();
            softly.assertThat(feature.installation.resolve("robozonky.cli"))
                .exists();
            softly.assertThat(feature.installation.resolve("robozonky-exec.bat"))
                .exists();
            softly.assertThat(feature.installation.resolve("robozonky-exec.sh"))
                .doesNotExist();
            softly.assertThat(feature.installation.resolve("robozonky-systemd.service"))
                .doesNotExist();
            softly.assertThat(feature.installation.resolve("robozonky.keystore"))
                .exists();
        });
        // test CLI contents
        String cliContents = Files.readString(feature.installation.resolve("robozonky.cli"));
        assertThat(cliContents)
            .isEqualTo("-d\r\n"
                    + "-g\r\n" +
                    "\"" + feature.installation.resolve("robozonky.keystore") + "\"\r\n"
                    + "-p\r\n" +
                    "\"" + String.valueOf(feature.secret) + "\"\r\n"
                    + "-s\r\n" +
                    "\"" + feature.strategyLocation + "\"\r\n"
                    + "-i\r\n" +
                    "\"" + feature.notificationLocation + "\"");
        // test exec contents
        String execContents = Files.readString(feature.installation.resolve("robozonky-exec.bat"));
        assertThat(execContents)
            .contains(feature.distribution.toString())
            .contains("robozonky.bat")
            .contains("Xmx128m")
            .contains("XX:StartFlightRecording")
            .contains("-Dcom.sun.management.jmxremote=\"true\"");
    }

    @Test
    void setupUnix() throws IOException, SetupFailedException {
        // Setup stuff.
        InstallationFeature feature = new InstallationFeature();
        feature.distribution = Files.createTempDirectory("robozonky-distribution");
        feature.installation = Files.createTempDirectory("robozonky-install");
        feature.keystore = Files.createTempFile("robozonky-", ".keystore");
        feature.secret = UUID.randomUUID()
            .toString()
            .toCharArray();
        feature.strategyLocation = new URL("http://somewhere.props");
        feature.notificationLocation = new URL("http://somewhere.cfg");
        feature.username = "someone@somewhere.cz";
        // Run the test.
        feature.setup();
        feature.test();
        // Check output.
        assertSoftly(softly -> {
            softly.assertThat(feature.installation.resolve("robozonky.properties"))
                .exists();
            softly.assertThat(feature.installation.resolve("management.properties"))
                .doesNotExist();
            softly.assertThat(feature.installation.resolve("robozonky.cli"))
                .exists();
            softly.assertThat(feature.installation.resolve("robozonky-exec.bat"))
                .doesNotExist();
            softly.assertThat(feature.installation.resolve("robozonky-exec.sh"))
                .exists();
            softly.assertThat(feature.installation.resolve("robozonky-systemd.service"))
                .exists();
            softly.assertThat(feature.installation.resolve("robozonky.keystore"))
                .exists();
        });
        // test CLI contents
        String cliContents = Files.readString(feature.installation.resolve("robozonky.cli"));
        assertThat(cliContents)
            .isEqualTo("-g\n" +
                    "\"" + feature.installation.resolve("robozonky.keystore") + "\"\n"
                    + "-p\n" +
                    "\"" + String.valueOf(feature.secret) + "\"\n"
                    + "-s\n" +
                    "\"" + feature.strategyLocation + "\"\n"
                    + "-i\n" +
                    "\"" + feature.notificationLocation + "\"");
        // test exec contents
        String execContents = Files.readString(feature.installation.resolve("robozonky-exec.sh"));
        assertThat(execContents)
            .contains(feature.distribution.toString())
            .contains("robozonky.sh")
            .contains("Xmx64m")
            .doesNotContain("XX:StartFlightRecording")
            .contains("-Dcom.sun.management.jmxremote=\"false\"");

    }

}
