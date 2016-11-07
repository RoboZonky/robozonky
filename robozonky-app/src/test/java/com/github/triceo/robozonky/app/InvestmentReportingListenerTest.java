/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.events.InvestmentMadeEvent;
import com.github.triceo.robozonky.remote.Investment;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class InvestmentReportingListenerTest {

    private final Collection<Path> availableFilesBeforeTestStart = new LinkedHashSet<>();

    private static List<Path> getFilesInWorkingDirectory() {
        final File folder = new File(System.getProperty("user.dir"));
        final File[] files = folder.listFiles();
        return Arrays.asList(files).stream()
                .filter(f -> !f.isDirectory())
                .map(File::toPath)
                .collect(Collectors.toList());
    }

    private static List<Path> getNewFilesInWorkingDirectory(final Collection<Path> original) {
        return InvestmentReportingListenerTest.getFilesInWorkingDirectory().stream()
                .filter(p -> !original.contains(p))
                .collect(Collectors.toList());
    }

    @Before
    public void loadAllFilesInWorkingDirectory() {
        availableFilesBeforeTestStart.clear();
        availableFilesBeforeTestStart.addAll(InvestmentReportingListenerTest.getFilesInWorkingDirectory());
    }

    @Test
    public void checkInvestmentReportedInStandardRun() throws IOException {
        checkInvestmentReported(false);
    }

    @Test
    public void checkInvestmentReportedInDryRun() throws IOException {
        checkInvestmentReported(true);
    }

    private void checkInvestmentReported(final boolean isDryRun) throws IOException {
        // prepare mock event
        final Investment mock = Mockito.mock(Investment.class);
        Mockito.when(mock.getLoanId()).thenReturn(1);
        Mockito.when(mock.getAmount()).thenReturn(2);
        final String expectedResult = "#" + mock.getLoanId() + ": " + mock.getAmount() + " CZK";
        final InvestmentMadeEvent evt = new InvestmentMadeEvent(mock);
        // run class under test
        new InvestmentReportingListener(isDryRun).handle(evt);
        // check existence and contents of new file
        final List<Path> newFiles =
                InvestmentReportingListenerTest.getNewFilesInWorkingDirectory(this.availableFilesBeforeTestStart);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(newFiles).hasSize(1);
        final Path p = newFiles.get(0);
        final String suffix = isDryRun ? InvestmentReportingListener.SUFFIX_DRY_RUN : InvestmentReportingListener
                .SUFFIX_REAL_RUN;
        softly.assertThat(p.toString()).endsWith(suffix);
        softly.assertThat(Files.lines(p)).containsExactly(expectedResult);
        softly.assertAll();
    }

    @After
    public void cleanWorkingDirectory() {
        final Collection<Path> files =
                InvestmentReportingListenerTest.getNewFilesInWorkingDirectory(this.availableFilesBeforeTestStart);
        files.forEach(p -> p.toFile().delete());
    }

}
