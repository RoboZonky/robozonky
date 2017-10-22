/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.installer.panels;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.UUID;

import com.github.robozonky.internal.api.Defaults;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.event.InstallerListener;
import com.izforge.izpack.api.event.ProgressListener;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.mockito.internal.verification.VerificationModeFactory.times;

public class RoboZonkyInstallerListenerTest {

    private static final String ZONKY_PASSWORD = UUID.randomUUID().toString();
    private static final String ZONKY_USERNAME = "user@zonky.cz";

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
        final InstallData data = Mockito.mock(InstallData.class);
        Mockito.when(data.getVariable(Variables.INSTALL_PATH.getKey()))
                .thenReturn(new File("target/install").getAbsolutePath());
        return data;
    }

    private static InstallData mockData() {
        final InstallData data = RoboZonkyInstallerListenerTest.mockBaseData();
        Mockito.when(data.getVariable(Variables.JAVA_HOME.getKey()))
                .thenReturn(System.getProperty("JAVA_HOME"));
        Mockito.when(data.getVariable(Variables.STRATEGY_TYPE.getKey()))
                .thenReturn("file");
        Mockito.when(data.getVariable(Variables.STRATEGY_SOURCE.getKey()))
                .thenReturn(RoboZonkyInstallerListenerTest.newFile(true).getAbsolutePath());
        Mockito.when(data.getVariable(Variables.ZONKY_USERNAME.getKey()))
                .thenReturn(RoboZonkyInstallerListenerTest.ZONKY_USERNAME);
        Mockito.when(data.getVariable(Variables.ZONKY_PASSWORD.getKey()))
                .thenReturn(RoboZonkyInstallerListenerTest.ZONKY_PASSWORD);
        Mockito.when(data.getVariable(Variables.IS_EMAIL_ENABLED.getKey())).thenReturn("true");
        Mockito.when(data.getVariable(Variables.SMTP_HOSTNAME.getKey())).thenReturn("127.0.0.1");
        Mockito.when(data.getVariable(Variables.SMTP_TO.getKey())).thenReturn("recipient@server.cz");
        Mockito.when(data.getVariable(Variables.SMTP_USERNAME.getKey())).thenReturn("sender@server.cz");
        Mockito.when(data.getVariable(Variables.SMTP_PASSWORD.getKey())).thenReturn(UUID.randomUUID().toString());
        return data;
    }

    private final InstallData data = RoboZonkyInstallerListenerTest.mockData();

    @Before
    public void createStructure() throws IOException {
        final File installDir = new File(Variables.INSTALL_PATH.getValue(data));
        final File distDir = new File(installDir, "Dist/");
        distDir.mkdirs();
        final File logback = new File(distDir, "logback.xml");
        logback.createNewFile();
        final File strategy = new File(Variables.STRATEGY_SOURCE.getValue(data));
        strategy.createNewFile();
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

    @After
    public void tearDownStructure() throws IOException {
        final File folder = new File(Variables.INSTALL_PATH.getValue(data));
        RoboZonkyInstallerListenerTest.deleteDir(folder);
    }

    @Test
    public void emailDisabled() {
        // prepare
        final InstallData data = RoboZonkyInstallerListenerTest.mockBaseData();
        Mockito.when(data.getVariable(Variables.IS_EMAIL_ENABLED.getKey())).thenReturn("false");
        RoboZonkyInstallerListener.setInstallData(data);
        // execute SUT
        final CommandLinePart clp = new RoboZonkyInstallerListener().prepareEmailConfiguration();
        // test
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(clp.getProperties()).isEmpty();
            softly.assertThat(RoboZonkyInstallerListener.EMAIL_CONFIG_FILE).doesNotExist();
        });
    }

    @Test
    public void emailEnabled() {
        // prepare
        final InstallData localData = RoboZonkyInstallerListenerTest.mockData();
        Mockito.when(localData.getVariable(Variables.IS_EMAIL_ENABLED.getKey())).thenReturn("true");
        RoboZonkyInstallerListener.setInstallData(localData);
        // execute SUT
        final CommandLinePart clp = new RoboZonkyInstallerListener().prepareEmailConfiguration();
        // test
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(clp.getProperties()).isNotEmpty();
            softly.assertThat(RoboZonkyInstallerListener.EMAIL_CONFIG_FILE).canRead();
        });
    }

    @Test
    public void strategyFile() {
        // prepare
        RoboZonkyInstallerListener.setInstallData(data);
        // execute SUT
        final CommandLinePart clp = new RoboZonkyInstallerListener().prepareStrategy();
        // test
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(clp.getOptions()).containsKey("-s");
            final File newStrat = new File(data.getVariable(Variables.INSTALL_PATH.getKey()), "robozonky-strategy.cfg");
            softly.assertThat(newStrat).exists();
        });
    }

    @Test
    public void coreWithoutKeyStore() {
        // prepare
        final InstallData localData = RoboZonkyInstallerListenerTest.mockData();
        Mockito.when(localData.getVariable(Variables.IS_ZONKOID_ENABLED.getKey())).thenReturn("true");
        Mockito.when(localData.getVariable(Variables.ZONKOID_TOKEN.getKey())).thenReturn("123456");
        RoboZonkyInstallerListener.setInstallData(localData);
        // execute SUT
        final CommandLinePart clp = new RoboZonkyInstallerListener().prepareCore(null);
        // test
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(clp.getOptions())
                    .doesNotContainKey("-d")
                    .doesNotContainKey("-r")
                    .doesNotContainKey("-g");
            softly.assertThat(clp.getOptions().get("-u"))
                    .containsOnly(localData.getVariable(Variables.ZONKY_USERNAME.getKey()));
            softly.assertThat(clp.getOptions().get("-x"))
                    .containsOnly("zonkoid:" + localData.getVariable(Variables.ZONKOID_TOKEN.getKey()));
            softly.assertThat(clp.getOptions().get("-p"))
                    .containsOnly(data.getVariable(Variables.ZONKY_PASSWORD.getKey()));
        });
    }

    @Test
    public void coreWithoutTweaks() {
        // prepare
        RoboZonkyInstallerListener.setInstallData(data);
        // execute SUT
        final CommandLinePart clp = new RoboZonkyInstallerListener().prepareCore();
        // test
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(clp.getOptions())
                    .doesNotContainKey("-d")
                    .doesNotContainKey("-r")
                    .doesNotContainKey("-x");
            softly.assertThat(clp.getOptions().get("-p"))
                    .containsOnly(String.valueOf(RoboZonkyInstallerListener.KEYSTORE_PASSWORD));
        });
    }

    @Test
    public void coreWithTweaks() {
        // prepare
        final InstallData localData = RoboZonkyInstallerListenerTest.mockData();
        Mockito.when(localData.getVariable(Variables.IS_DRY_RUN.getKey())).thenReturn("true");
        Mockito.when(localData.getVariable(Variables.IS_USING_OAUTH_TOKEN.getKey())).thenReturn("true");
        Mockito.when(localData.getVariable(Variables.IS_ZONKOID_ENABLED.getKey())).thenReturn("true");
        Mockito.when(localData.getVariable(Variables.ZONKOID_TOKEN.getKey())).thenReturn("123456");
        RoboZonkyInstallerListener.setInstallData(localData);
        // execute SUT
        final CommandLinePart clp = new RoboZonkyInstallerListener().prepareCore();
        // test
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(clp.getOptions())
                    .containsKey("-d")
                    .containsKey("-r")
                    .containsKey("-x");
            softly.assertThat(clp.getOptions().get("-p"))
                    .containsOnly(String.valueOf(RoboZonkyInstallerListener.KEYSTORE_PASSWORD));
        });
    }

    @Test
    public void jmx() {
        // prepare
        RoboZonkyInstallerListener.setInstallData(data);
        Mockito.when(data.getVariable(Variables.IS_JMX_ENABLED.getKey())).thenReturn("true");
        Mockito.when(data.getVariable(Variables.IS_JMX_SECURITY_ENABLED.getKey())).thenReturn("false");
        Mockito.when(data.getVariable(Variables.JMX_PORT.getKey())).thenReturn("1234");
        Mockito.when(data.getVariable(Variables.JMX_HOSTNAME.getKey())).thenReturn("somewhere");
        // execute SUT
        final CommandLinePart clp = new RoboZonkyInstallerListener().prepareJmx();
        // test
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(clp.getProperties().get("com.sun.management.jmxremote"))
                    .isEqualTo("true");
            softly.assertThat(clp.getProperties().get("com.sun.management.config.file"))
                    .isEqualTo(RoboZonkyInstallerListener.JMX_PROPERTIES_FILE.getAbsolutePath());
            softly.assertThat(RoboZonkyInstallerListener.JMX_PROPERTIES_FILE).exists();
        });
    }

    @Test
    public void strategyUrl() {
        // prepare
        final InstallData localData = RoboZonkyInstallerListenerTest.mockBaseData();
        Mockito.when(localData.getVariable(Variables.STRATEGY_TYPE.getKey())).thenReturn("url");
        Mockito.when(localData.getVariable(Variables.STRATEGY_SOURCE.getKey())).thenReturn("http://www.robozonky.cz");
        RoboZonkyInstallerListener.setInstallData(localData);
        // execute SUT
        final CommandLinePart clp = new RoboZonkyInstallerListener().prepareStrategy();
        // test
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(clp.getOptions()).containsKey("-s");
            final File newStrat = new File(data.getVariable(Variables.INSTALL_PATH.getKey()), "robozonky-strategy.cfg");
            softly.assertThat(newStrat).doesNotExist();
        });
    }

    @Test
    public void progressUnix() {
        final ProgressListener progress = Mockito.mock(ProgressListener.class);
        // execute SUT
        RoboZonkyInstallerListener.setInstallData(data);
        final InstallerListener listener = new RoboZonkyInstallerListener();
        listener.afterPacks(Collections.emptyList(), progress);
        // test
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "logback.xml")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky.properties")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky.cli")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "run.bat")).doesNotExist();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "run.sh")).exists();
            softly.assertThat(RoboZonkyInstallerListener.CLI_CONFIG_FILE).exists();
        });
        Mockito.verify(progress, times(1)).startAction(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());
        Mockito.verify(progress, times(7))
                .nextStep(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.eq(1));
        Mockito.verify(progress, times(1)).stopAction();
    }

    @Test
    public void progressWindows() {
        final ProgressListener progress = Mockito.mock(ProgressListener.class);
        final InstallData localData = RoboZonkyInstallerListenerTest.mockData();
        Mockito.when(localData.getVariable(Variables.IS_WINDOWS.getKey())).thenReturn("true");
        // execute SUT
        RoboZonkyInstallerListener.setInstallData(localData);
        final InstallerListener listener = new RoboZonkyInstallerListener();
        listener.afterPacks(Collections.emptyList(), progress);
        // test
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "logback.xml")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky.properties")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "robozonky.cli")).exists();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "run.sh")).doesNotExist();
            softly.assertThat(new File(RoboZonkyInstallerListener.INSTALL_PATH, "run.bat")).exists();
            softly.assertThat(RoboZonkyInstallerListener.CLI_CONFIG_FILE).exists();
        });
        Mockito.verify(progress, times(1)).startAction(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt());
        Mockito.verify(progress, times(7))
                .nextStep(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.eq(1));
        Mockito.verify(progress, times(1)).stopAction();
    }
}
