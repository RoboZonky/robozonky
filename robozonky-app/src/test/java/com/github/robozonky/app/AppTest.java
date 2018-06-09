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

package com.github.robozonky.app;

import java.util.List;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.api.notifications.RoboZonkyStartingEvent;
import com.github.robozonky.test.exit.TestingSystemExit;
import com.github.robozonky.test.exit.TestingSystemExitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class AppTest extends AbstractEventLeveragingTest {

    private static final TestingSystemExit EXIT = TestingSystemExitService.INSTANCE;

    @BeforeEach
    @AfterEach
    void reset() {
        EXIT.reset();
    }

    @Test
    void notWellFormedCli() {
        App.main();
        assertThat(EXIT.getReturnCode()).hasValue(ReturnCode.ERROR_WRONG_PARAMETERS.getCode());
    }

    @Test
    void help() {
        App.main("-h");
        assertThat(EXIT.getReturnCode()).hasValue(ReturnCode.OK.getCode());
    }

    @Test
    void proper() {
        App.main("-u", "someone", "-p", "password", "test");
        final List<Event> events = getNewEvents();
        assertSoftly(softly -> {
            softly.assertThat(EXIT.getReturnCode()).hasValue(ReturnCode.OK.getCode());
            softly.assertThat(events).hasSize(3);
        });
        assertSoftly(softly -> {
            softly.assertThat(events.get(0)).isInstanceOf(RoboZonkyStartingEvent.class);
            softly.assertThat(events.get(1)).isInstanceOf(RoboZonkyInitializedEvent.class);
            softly.assertThat(events.get(2)).isInstanceOf(RoboZonkyEndingEvent.class);
        });
    }
}

