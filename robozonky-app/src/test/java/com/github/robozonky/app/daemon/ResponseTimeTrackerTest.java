/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.Settings;
import com.github.robozonky.internal.remote.entities.ParticipationImpl;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.github.robozonky.test.mock.MockLoanBuilder;

class ResponseTimeTrackerTest extends AbstractRoboZonkyTest {

    @BeforeEach
    void enableDebug() {
        System.setProperty(Settings.Key.DEBUG_ENABLE_DAEMON_TIMING.getName(), "true");
    }

    @BeforeEach
    @AfterEach
    void deleteFiles() throws IOException {
        ResponseTimeTracker.executeAsync((r, time) -> r.clear())
            .join(); // To make sure everything is written before the next test starts.
        Files.deleteIfExists(ResponseTimeTracker.LOAN_OUTPUT_PATH);
        Files.deleteIfExists(ResponseTimeTracker.PARTICIPATION_OUTPUT_PATH);
    }

    @Test
    void testLoan() {
        var expectedResult = new AtomicLong();
        var loan = MockLoanBuilder.fresh();
        ResponseTimeTracker.executeAsync((r, time) -> {
            r.registerLoan(time, loan.getId());
            expectedResult.set(time);
        })
            .join();
        assertThat(ResponseTimeTracker.LOAN_OUTPUT_PATH)
            .doesNotExist();
        ResponseTimeTracker.executeAsync((r, time) -> {
            r.dispatch(time, loan);
            expectedResult.getAndUpdate((orig) -> time - orig);
        })
            .join();
        assertThat(ResponseTimeTracker.LOAN_OUTPUT_PATH)
            .doesNotExist();
        ResponseTimeTracker.executeAsync((r, time) -> r.clear())
            .join();
        assertThat(ResponseTimeTracker.LOAN_OUTPUT_PATH)
            .hasContent(loan.getId() + " " + expectedResult.longValue());
        assertThat(ResponseTimeTracker.PARTICIPATION_OUTPUT_PATH)
            .doesNotExist();
    }

    @Test
    void testParticipation() {
        var expectedResult = new AtomicLong();
        var participation = new ParticipationImpl();
        participation.setId(12345);
        ResponseTimeTracker.executeAsync((r, time) -> {
            r.registerParticipation(time, participation.getId());
            expectedResult.set(time);
        })
            .join();
        assertThat(ResponseTimeTracker.PARTICIPATION_OUTPUT_PATH)
            .doesNotExist();
        ResponseTimeTracker.executeAsync((r, time) -> {
            r.dispatch(time, participation);
            expectedResult.getAndUpdate((orig) -> time - orig);
        })
            .join();
        assertThat(ResponseTimeTracker.PARTICIPATION_OUTPUT_PATH)
            .doesNotExist();
        ResponseTimeTracker.executeAsync((r, time) -> r.clear())
            .join();
        assertThat(ResponseTimeTracker.PARTICIPATION_OUTPUT_PATH)
            .hasContent(participation.getId() + " " + expectedResult.longValue());
        assertThat(ResponseTimeTracker.LOAN_OUTPUT_PATH)
            .doesNotExist();
    }

}
