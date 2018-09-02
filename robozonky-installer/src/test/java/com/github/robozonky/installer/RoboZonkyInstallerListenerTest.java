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

package com.github.robozonky.installer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.UUID;

import com.github.robozonky.cli.SetupFailedException;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.event.InstallerListener;
import com.izforge.izpack.api.event.ProgressListener;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class RoboZonkyInstallerListenerTest extends AbstractRoboZonkyTest {

    private static final String ZONKY_PASSWORD = UUID.randomUUID().toString();
    private static final String ZONKY_USERNAME = "user@zonky.cz";

    private final InstallData data = RoboZonkyInstallerListenerTest.mockData();

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

    private static InstallData mockBaseData() {
        final InstallData data = mock(InstallData.class);
        when(data.getVariable(Variables.INSTALL_PATH.getKey()))
                .thenReturn(new File("target/install").getAbsolutePath());
        return data;
    }

    private static InstallData mockData() {
        final InstallData data = RoboZonkyInstallerListenerTest.mockBaseData();
        when(data.getVariable(Variables.STRATEGY_TYPE.getKey())).thenReturn("file");
        when(data.getVariable(Variables.STRATEGY_SOURCE.getKey()))
                .thenReturn(RoboZonkyInstallerListenerTest.newFile(true).getAbsolutePath());
        when(data.getVariable(Variables.ZONKY_USERNAME.getKey()))
                .thenReturn(RoboZonkyInstallerListenerTest.ZONKY_USERNAME);
        when(data.getVariable(Variables.ZONKY_PASSWORD.getKey()))
                .thenReturn(RoboZonkyInstallerListenerTest.ZONKY_PASSWORD);
        when(data.getVariable(Variables.IS_EMAIL_ENABLED.getKey())).thenReturn("true");
        when(data.getVariable(Variables.SMTP_HOSTNAME.getKey())).thenReturn("127.0.0.1");
        when(data.getVariable(Variables.SMTP_TO.getKey())).thenReturn("recipient@server.cz");
        when(data.getVariable(Variables.SMTP_USERNAME.getKey())).thenReturn("sender@server.cz");
        when(data.getVariable(Variables.SMTP_PASSWORD.getKey())).thenReturn(UUID.randomUUID().toString());
        // otherwise browser window will open to authenticate with Google
        when(data.getVariable(Variables.IS_STONKY_ENABLED.getKey())).thenReturn("false");
        when(data.getVariable(Variables.GOOGLE_CALLBACK_HOST.getKey())).thenReturn("localhost");
        when(data.getVariable(Variables.GOOGLE_CALLBACK_PORT.getKey())).thenReturn("0");
        return data;
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
        final File distDir = new File(installDir, "Dist/");
        distDir.mkdirs();
        final File logback = new File(distDir, "logback.xml");
        logback.createNewFile();
        final File strategy = new File(Variables.STRATEGY_SOURCE.getValue(data));
        strategy.createNewFile();
    }

    @AfterEach
    void tearDownStructure() {
        final File folder = new File(Variables.INSTALL_PATH.getValue(data));
        RoboZonkyInstallerListenerTest.deleteDir(folder);
    }

    @Test
    void emailDisabled() {
        // prepare
        final InstallData data = RoboZonkyInstallerListenerTest.mockBaseData();
        when(data.getVariable(Variables.IS_EMAIL_ENABLED.getKey())).thenReturn("false");
        RoboZonkyInstallerListener.setInstallData(data);
        // execute SUT
        final CommandLinePart clp = RoboZonkyInstallerListener.prepareEmailConfiguration();
        // test
        assertSoftly(softly -> {
            softly.assertThat(clp.getProperties()).isEmpty();
            softly.assertThat(RoboZonkyInstallerListener.EMAIL_CONFIG_FILE).doesNotExist();
        });
    }

    @Test
    void emailEnabled() {
        // prepare
        final InstallData localData = RoboZonkyInstallerListenerTest.mockData();
        when(localData.getVariable(Variables.IS_EMAIL_ENABLED.getKey())).thenReturn("true");
        RoboZonkyInstallerListener.setInstallData(localData);
        // execute SUT
        final CommandLinePart clp = RoboZonkyInstallerListener.prepareEmailConfiguration();
        // test
        assertSoftly(softly -> {
            softly.assertThat(clp.getOptions()).containsOnlyKeys("-i");
            softly.assertThat(RoboZonkyInstallerListener.EMAIL_CONFIG_FILE).canRead();
        });
    }

    @Test
    void strategyFile() {
        // prepare
        RoboZonkyInstallerListener.setInstallData(data);
        // execute SUT
        final CommandLinePart clp = RoboZonkyInstallerListener.prepareStrategy();
        // test
        assertSoftly(softly -> {
            softly.assertThat(clp.getOptions()).containsKey("-s");
            final File newStrat = new File(data.getVariable(Variables.INSTALL_PATH.getKey()), "robozonky-strategy.cfg");
            softly.assertThat(newStrat).exists();
        });
    }

    @Test
    void coreWithoutTweaks() throws SetupFailedException {
        // prepare
        RoboZonkyInstallerListener.setInstallData(data);
        // execute SUT
        final CommandLinePart clp = RoboZonkyInstallerListener.prepareCore();
        // test
        assertSoftly(softly -> {
            softly.assertThat(clp.getOptions())
                    .doesNotContainKey("-d")
                    .doesNotContainKey("-r")
                    .doesNotContainKey("-x");
            softly.assertThat(clp.getOptions().get("-p"))
                    .containsOnly(String.valueOf(RoboZonkyInstallerListener.KEYSTORE_PASSWORD));
        });
    }

    @Test
    void coreWithTweaks() throws SetupFailedException {
        // prepare
        final InstallData localData = RoboZonkyInstallerListenerTest.mockData();
        when(localData.getVariable(Variables.IS_DRY_RUN.getKey())).thenReturn("true");
        when(localData.getVariable(Variables.IS_ZONKOID_ENABLED.getKey())).thenReturn("true");
        when(localData.getVariable(Variables.ZONKOID_TOKEN.getKey())).thenReturn("123456");
        RoboZonkyInstallerListener.setInstallData(localData);
        // execute SUT
        final CommandLinePart clp = RoboZonkyInstallerListener.prepareCore();
        // test
        assertSoftly(softly -> {
            softly.assertThat(clp.getOptions())
                    .containsKey("-d")
                    .containsKey("-x");
            softly.assertThat(clp.getOptions().get("-p"))
                    .containsOnly(String.valueOf(RoboZonkyInstallerListener.KEYSTORE_PASSWORD));
        });
    }

    @Test
    void jmx() {
        // prepare
        RoboZonkyInstallerListener.setInstallData(data);
        when(data.getVariable(Variables.IS_JMX_ENABLED.getKey())).thenReturn("true");
        when(data.getVariable(Variables.IS_JMX_SECURITY_ENABLED.getKey())).thenReturn("false");
        when(data.getVariable(Variables.JMX_PORT.getKey())).thenReturn("1234");
        when(data.getVariable(Variables.JMX_HOSTNAME.getKey())).thenReturn("somewhere");
        // execute SUT
        final CommandLinePart clp = RoboZonkyInstallerListener.prepareJmx();
        // test
        assertSoftly(softly -> {
            softly.assertThat(clp.getProperties().get("com.sun.management.jmxremote"))
                    .isEqualTo("true");
            softly.assertThat(clp.getProperties().get("com.sun.management.config.file"))
                    .isEqualTo(RoboZonkyInstallerListener.JMX_PROPERTIES_FILE.getAbsolutePath());
            softly.assertThat(RoboZonkyInstallerListener.JMX_PROPERTIES_FILE).exists();
        });
    }

    @Test
    void strategyUrl() {
        // prepare
        final InstallData localData = RoboZonkyInstallerListenerTest.mockBaseData();
        when(localData.getVariable(Variables.STRATEGY_TYPE.getKey())).thenReturn("url");
        when(localData.getVariable(Variables.STRATEGY_SOURCE.getKey())).thenReturn("http://www.robozonky.cz");
        RoboZonkyInstallerListener.setInstallData(localData);
        // execute SUT
        final CommandLinePart clp = RoboZonkyInstallerListener.prepareStrategy();
        // test
        assertSoftly(softly -> {
            softly.assertThat(clp.getOptions()).containsKey("-s");
            final File newStrat = new File(data.getVariable(Variables.INSTALL_PATH.getKey()), "robozonky-strategy.cfg");
            softly.assertThat(newStrat).doesNotExist();
        });
    }

    @Test
    void progressUnix() {
        final ProgressListener progress = mock(ProgressListener.class);
        // execute SUT
        RoboZonkyInstallerListener.setInstallData(data);
        final InstallerListener listener = new RoboZonkyInstallerListener(RoboZonkyInstallerListener.OS.LINUX);
        listener.afterPacks(Collections.emptyList(), progress);
        // test
        assertSoftly(softly -> {
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "logback.xml")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky.properties")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky.cli")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky-exec.bat")).doesNotExist();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky-exec.sh"))
                    .exists()
                    .has(new Condition<>(File::canExecute, "Is executable"));
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky-systemd.service")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky-upstart.conf")).exists();
            softly.assertThat(RoboZonkyInstallerListener.CLI_CONFIG_FILE).exists();
        });
        verify(progress, times(1)).startAction(anyString(), anyInt());
        verify(progress, times(8))
                .nextStep(anyString(), anyInt(), eq(1));
        verify(progress, times(1)).stopAction();
    }

    @Test
    void progressWindows() {
        final ProgressListener progress = mock(ProgressListener.class);
        final InstallData localData = RoboZonkyInstallerListenerTest.mockData();
        // execute SUT
        RoboZonkyInstallerListener.setInstallData(localData);
        final InstallerListener listener = new RoboZonkyInstallerListener(RoboZonkyInstallerListener.OS.WINDOWS);
        listener.afterPacks(Collections.emptyList(), progress);
        // test
        assertSoftly(softly -> {
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "logback.xml")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky.properties")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky.cli")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky-exec.sh")).doesNotExist();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky-exec.bat"))
                    .exists()
                    .has(new Condition<>(File::canExecute, "Is executable"));
            softly.assertThat(
                    new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky-systemd.service")).doesNotExist();
            softly.assertThat(
                    new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky-upstart.conf")).doesNotExist();
            softly.assertThat(RoboZonkyInstallerListener.CLI_CONFIG_FILE).exists();
        });
        verify(progress, times(1)).startAction(anyString(), anyInt());
        verify(progress, times(8))
                .nextStep(anyString(), anyInt(), eq(1));
        verify(progress, times(1)).stopAction();
    }
}
