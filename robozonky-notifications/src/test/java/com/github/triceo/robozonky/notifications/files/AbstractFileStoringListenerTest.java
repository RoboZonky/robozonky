/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.triceo.robozonky.api.notifications.SessionInfo;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class AbstractFileStoringListenerTest {

    private final Collection<Path> availableFilesBeforeTestStart = new LinkedHashSet<>();

    private static Collection<Path> getFilesInWorkingDirectory() {
        final File folder = new File(System.getProperty("user.dir"));
        final File[] files = folder.listFiles();
        return Arrays.stream(files)
                .filter(f -> !f.isDirectory())
                .map(File::toPath)
                .collect(Collectors.toList());
    }

    private static List<Path> getNewFilesInWorkingDirectory(final Collection<Path> original) {
        return AbstractFileStoringListenerTest.getFilesInWorkingDirectory().stream()
                .filter(p -> !original.contains(p))
                .collect(Collectors.toList());
    }

    private static FileNotificationProperties getNotificationProperties() {
        System.setProperty(RefreshableFileNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
                AbstractFileStoringListener.class.getResource("notifications-enabled.cfg").toString());
        final Refreshable<FileNotificationProperties> r = new RefreshableFileNotificationProperties();
        r.run();
        final Optional<FileNotificationProperties> p = r.getLatest();
        System.clearProperty(RefreshableFileNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY);
        return p.get();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getListenerInfo() {
        final Collection<Object[]> result = new ArrayList<>();
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getId()).thenReturn(1000);
        Mockito.when(l.getRemainingInvestment()).thenReturn(Double.MAX_VALUE);
        Mockito.when(l.getDatePublished()).thenReturn(OffsetDateTime.now());
        final FileNotificationProperties p = AbstractFileStoringListenerTest.getNotificationProperties();
        final Investment i = new Investment(l, 300);
        final InvestmentMadeEvent ime = new InvestmentMadeEvent(i, 0, false);
        final EventListener l1 = SupportedFileListener.INVESTMENT_MADE.getListener(p);
        result.add(new Object[] {l1, ime, i.getLoanId(), i.getAmount(), "invested"} );
        final Recommendation recommendation = new LoanDescriptor(l).recommend(300).get();
        final InvestmentRejectedEvent ire = new InvestmentRejectedEvent(recommendation, Integer.MAX_VALUE, "random");
        final EventListener l2 = SupportedFileListener.INVESTMENT_REJECTED.getListener(p);
        result.add(new Object[] {l2, ire, recommendation.getLoanDescriptor().getLoan().getId(),
                recommendation.getRecommendedInvestmentAmount(), "rejected"} );
        final InvestmentDelegatedEvent ide = new InvestmentDelegatedEvent(recommendation, 0, "random");
        final EventListener l3 = SupportedFileListener.INVESTMENT_DELEGATED.getListener(p);
        result.add(new Object[] {l3, ide, recommendation.getLoanDescriptor().getLoan().getId(),
                recommendation.getRecommendedInvestmentAmount(), "delegated"} );
        final InvestmentSkippedEvent ise = new InvestmentSkippedEvent(recommendation);
        final EventListener l4 = SupportedFileListener.INVESTMENT_SKIPPED.getListener(p);
        result.add(new Object[] {l4, ise, recommendation.getLoanDescriptor().getLoan().getId(),
                recommendation.getRecommendedInvestmentAmount(), "skipped"} );
        return result;
    }

    @Parameterized.Parameter
    public AbstractFileStoringListener listener;
    @Parameterized.Parameter(1)
    public Event event;
    @Parameterized.Parameter(2)
    public int loanId;
    @Parameterized.Parameter(3)
    public int loanAmount;
    @Parameterized.Parameter(4)
    public String suffix;

    @Before
    public void loadAllFilesInWorkingDirectory() {
        availableFilesBeforeTestStart.clear();
        availableFilesBeforeTestStart.addAll(AbstractFileStoringListenerTest.getFilesInWorkingDirectory());
    }

    @Test
    public void checkInvestmentReported() throws IOException {
        final int preCounter1 = listener.getFilesOfThisType().getCount();
        final int preCounter2 = listener.getProperties().getGlobalCounter().getCount();
        // run class under test
        final Collection<Path> oldFiles = AbstractFileStoringListenerTest.getFilesInWorkingDirectory();
        listener.handle(event, new SessionInfo(""));
        final List<Path> newFiles = AbstractFileStoringListenerTest.getNewFilesInWorkingDirectory(oldFiles);
        // check existence and contents of new file
        Assertions.assertThat(newFiles).hasSize(1);
        final Path p = newFiles.get(0);
        Assertions.assertThat(p.toString()).endsWith(this.suffix);
        final String expectedResult = "#" + this.loanId + ": " + this.loanAmount + " CZK";
        Assertions.assertThat(Files.lines(p)).containsExactly(expectedResult);
        Assertions.assertThat(listener.getFilesOfThisType().getCount()).isGreaterThan(preCounter1);
        Assertions.assertThat(listener.getProperties().getGlobalCounter().getCount()).isGreaterThan(preCounter2);
    }

    @After
    public void restoreWorkingDirectoryToOriginalState() {
        final Collection<Path> files =
                AbstractFileStoringListenerTest.getNewFilesInWorkingDirectory(this.availableFilesBeforeTestStart);
        files.forEach(p -> p.toFile().delete());
    }

}
