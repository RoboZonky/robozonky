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

package com.github.robozonky.installer;

import static com.github.robozonky.installer.RoboZonkyInstallerListener.INSTALL_PATH;
import static com.github.robozonky.installer.RoboZonkyInstallerListener.setInstallData;
import static com.github.robozonky.installer.RoboZonkyInstallerListener.setKeystoreInformation;
import static com.github.robozonky.installer.Variables.EMAIL_CONFIGURATION_TYPE;
import static com.github.robozonky.installer.Variables.IS_DRY_RUN;
import static com.github.robozonky.installer.Variables.IS_EMAIL_ENABLED;
import static com.github.robozonky.installer.Variables.SMTP_HOSTNAME;
import static com.github.robozonky.installer.Variables.SMTP_PASSWORD;
import static com.github.robozonky.installer.Variables.SMTP_TO;
import static com.github.robozonky.installer.Variables.SMTP_USERNAME;
import static com.github.robozonky.installer.Variables.STRATEGY_SOURCE;
import static com.github.robozonky.installer.Variables.STRATEGY_TYPE;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.event.InstallerListener;
import com.izforge.izpack.api.event.ProgressListener;

class RoboZonkyInstallerListenerTest extends AbstractRoboZonkyTest {

    private static final String ZONKY_PASSWORD = UUID.randomUUID()
        .toString();
    private static final String ZONKY_USERNAME = "user@zonky.cz";

    private final InstallData data = RoboZonkyInstallerListenerTest.mockData();

    @BeforeAll
    static void initialize() {
        final File f = newFile(false);
        setKeystoreInformation(f, ZONKY_PASSWORD.toCharArray());
    }

    private static File newFile(final boolean withContent) {
        try {
            final File f = File.createTempFile("robozonky-", ".tmp");
            if (withContent) {
                Files.write(f.toPath(), Collections.singleton("Content"), Defaults.CHARSET);
            }
            return f;
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed creating temp file.", ex);
        }
    }

    private static InstallData actuallyMockData() throws IOException {
        final InstallData data = mock(InstallData.class);
        Path path = Files.createTempDirectory("robozonky-install");
        when(data.getVariable(Variables.INSTALL_PATH.getKey()))
            .thenReturn(path.toAbsolutePath()
                .toString());
        when(data.getVariable(STRATEGY_TYPE.getKey())).thenReturn("file");
        when(data.getVariable(STRATEGY_SOURCE.getKey()))
            .thenReturn(RoboZonkyInstallerListenerTest.newFile(true)
                .getAbsolutePath());
        when(data.getVariable(Variables.ZONKY_USERNAME.getKey()))
            .thenReturn(RoboZonkyInstallerListenerTest.ZONKY_USERNAME);
        when(data.getVariable(Variables.ZONKY_PASSWORD.getKey()))
            .thenReturn(RoboZonkyInstallerListenerTest.ZONKY_PASSWORD);
        when(data.getVariable(IS_EMAIL_ENABLED.getKey())).thenReturn("true");
        when(data.getVariable(EMAIL_CONFIGURATION_TYPE.getKey())).thenReturn("custom");
        when(data.getVariable(SMTP_HOSTNAME.getKey())).thenReturn("127.0.0.1");
        when(data.getVariable(SMTP_TO.getKey())).thenReturn("recipie  nt@server.cz");
        when(data.getVariable(SMTP_USERNAME.getKey())).thenReturn("sender@server.cz");
        when(data.getVariable(SMTP_PASSWORD.getKey())).thenReturn(UUID.randomUUID()
            .toString());
        return data;
    }

    private static InstallData mockData() {
        try {
            return actuallyMockData();
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void deleteDir(final File file) {
        final File[] contents = file.listFiles();
        if (contents != null) {
            for (final File f : contents) {
                RoboZonkyInstallerListenerTest.deleteDir(f);
            }
        }
        file.delete();
    }

    @BeforeEach
    void createStructure() throws IOException {
        final File installDir = new File(Variables.INSTALL_PATH.getValue(data));
        if (installDir.exists()) {
            RoboZonkyInstallerListenerTest.deleteDir(installDir);
        }
        final File distDir = new File(installDir, "dist/");
        distDir.mkdirs();
        final File loggingConfig = new File(distDir, "log4j2.xml");
        loggingConfig.createNewFile();
        final File strategy = new File(STRATEGY_SOURCE.getValue(data));
        strategy.createNewFile();
    }

    @AfterEach
    void tearDownStructure() {
        final File folder = new File(Variables.INSTALL_PATH.getValue(data));
        RoboZonkyInstallerListenerTest.deleteDir(folder);
    }

    private void baseAssertions() {

    }

    @Test
    void progressRealUnix() throws IOException {
        final ProgressListener progress = mock(ProgressListener.class);
        // execute SUT
        setInstallData(data);
        final InstallerListener listener = new RoboZonkyInstallerListener(true);
        listener.afterPacks(Collections.emptyList(), progress);
        // test
        String installPath = INSTALL_PATH.getAbsolutePath();
        Path cli = INSTALL_PATH.toPath()
            .resolve("robozonky.cli");
        assertSoftly(softly -> {
            softly.assertThat(new File(installPath, "log4j2.xml"))
                .exists();
            softly.assertThat(new File(installPath, "robozonky.properties"))
                .exists();
            softly.assertThat(new File(installPath, "robozonky-strategy.cfg"))
                .exists();
            softly.assertThat(cli)
                .exists();
            softly.assertThat(new File(installPath, "robozonky-exec.bat"))
                .doesNotExist();
            softly.assertThat(new File(installPath, "robozonky-exec.sh"))
                .exists()
                .has(new Condition<>(File::canExecute, "Is executable"));
            softly.assertThat(new File(installPath, "robozonky-systemd.service"))
                .exists();
            softly.assertThat(new File(installPath, "robozonky.keystore"))
                .exists();
        });
        verify(progress, times(1)).startAction(anyString(), anyInt());
        verify(progress, times(5))
            .nextStep(anyString(), anyInt(), eq(1));
        verify(progress, times(1)).stopAction();
        // test CLI contents
        String cliContents = Files.readString(cli);
        assertThat(cliContents)
            .isEqualTo("-g \"" + installPath + "/robozonky.keystore\"\n"
                    + "-p \"" + ZONKY_PASSWORD + "\"\n"
                    + "-s \"" + installPath + "/robozonky-strategy.cfg\"\n"
                    + "-i \"file://" + installPath + "/robozonky-notifications.cfg\"");
    }

    @Test
    void progressDryWindows() throws IOException {
        final ProgressListener progress = mock(ProgressListener.class);
        // execute SUT
        setInstallData(data);
        when(data.getVariable(IS_DRY_RUN.getKey())).thenReturn("true");
        final InstallerListener listener = new RoboZonkyInstallerListener(false);
        listener.afterPacks(Collections.emptyList(), progress);
        // test
        String installPath = INSTALL_PATH.getAbsolutePath();
        Path cli = INSTALL_PATH.toPath()
            .resolve("robozonky.cli");
        assertSoftly(softly -> {
            softly.assertThat(new File(installPath, "log4j2.xml"))
                .exists();
            softly.assertThat(new File(installPath, "robozonky.properties"))
                .exists();
            softly.assertThat(new File(installPath, "robozonky-strategy.cfg"))
                .exists();
            softly.assertThat(cli)
                .exists();
            softly.assertThat(new File(installPath, "robozonky-exec.bat"))
                .exists();
            softly.assertThat(new File(installPath, "robozonky-exec.sh"))
                .doesNotExist();
            softly.assertThat(new File(installPath, "robozonky-systemd.service"))
                .doesNotExist();
            softly.assertThat(new File(installPath, "robozonky.keystore"))
                .exists();
        });
        verify(progress, times(1)).startAction(anyString(), anyInt());
        verify(progress, times(5))
            .nextStep(anyString(), anyInt(), eq(1));
        verify(progress, times(1)).stopAction();
        // test CLI contents
        String cliContents = Files.readString(cli);
        assertThat(cliContents)
            .isEqualTo("-d\r\n"
                    + "-g \"" + installPath + "/robozonky.keystore\"\r\n"
                    + "-p \"" + ZONKY_PASSWORD + "\"\r\n"
                    + "-s \"" + installPath + "/robozonky-strategy.cfg\"\r\n"
                    + "-i \"file://" + installPath + "/robozonky-notifications.cfg\"");
    }

}
