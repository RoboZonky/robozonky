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

package com.github.robozonky.app;

import java.util.List;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.api.notifications.RoboZonkyStartingEvent;
import com.github.robozonky.app.events.AbstractEventLeveragingTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class AppTest extends AbstractEventLeveragingTest {

    @Test
    void notWellFormedCli() {
        final App main = spy(new App());
        doNothing().when(main).actuallyExit(anyInt());
        main.run();
        verify(main).actuallyExit(eq(ReturnCode.ERROR_SETUP.getCode()));
    }

    @Test
    void help() {
        final App main = spy(new App("-h"));
        doNothing().when(main).actuallyExit(anyInt());
        main.run();
        verify(main).actuallyExit(eq(ReturnCode.ERROR_SETUP.getCode()));
    }

    @Test
    void wellFormedCliWithNonexistentKeystore() {
        final App main = spy(new App("-p", "a", "-g", "b", "-d", "-s", "a"));
        doNothing().when(main).actuallyExit(anyInt());
        main.run();
        verify(main).actuallyExit(eq(ReturnCode.ERROR_SETUP.getCode()));
    }

    /**
     * This is a hackish white-box test that tests various internals of the app implementation.
     */
    @Test
    void triggersEvents() {
        final App main = spy(new App());
        doNothing().when(main).actuallyExit(anyInt());
        try {
            assertThat(main.execute(l -> new MyInvestmentMode())).isEqualTo(ReturnCode.OK);
        } finally { // clean up, shutting down executors etc.
            main.exit(ReturnCode.OK);
        }
        verify(main).actuallyExit(ReturnCode.OK.getCode());
        final List<Event> events = getEventsRequested();
        assertThat(events).hasSize(3);
        assertSoftly(softly -> {
            softly.assertThat(events.get(0)).isInstanceOf(RoboZonkyStartingEvent.class);
            softly.assertThat(events.get(1)).isInstanceOf(RoboZonkyInitializedEvent.class);
            softly.assertThat(events.get(2)).isInstanceOf(RoboZonkyEndingEvent.class);
        });
    }

    /**
     * This is a hackish white-box test that tests various internals of the app implementation.
     */
    @Test
    void failsCorrectly() {
        final App main = spy(new App());
        doNothing().when(main).actuallyExit(anyInt());
        try {
            final ReturnCode result = main.execute(l -> new MyFailingInvestmentMode());
            assertThat(result).isEqualTo(ReturnCode.ERROR_UNEXPECTED);
        } finally { // clean up, shutting down executors etc.
            final List<Event> events = getEventsRequested();
            assertThat(events).hasSize(3);
            assertSoftly(softly -> {
                softly.assertThat(events.get(0)).isInstanceOf(RoboZonkyStartingEvent.class);
                softly.assertThat(events.get(1)).isInstanceOf(RoboZonkyInitializedEvent.class);
                softly.assertThat(events.get(2)).isInstanceOf(RoboZonkyEndingEvent.class);
            });
        }
    }

    private static class MyInvestmentMode implements InvestmentMode {

        @Override
        public SessionInfo getSessionInfo() {
            return mockSessionInfo();
        }

        @Override
        public ReturnCode get() {
            return ReturnCode.OK;
        }

        @Override
        public void close() {

        }
    }

    private static class MyFailingInvestmentMode implements InvestmentMode {

        @Override
        public SessionInfo getSessionInfo() {
            return mockSessionInfo();
        }

        @Override
        public ReturnCode get() {
            throw new IllegalStateException("Testing failure");
        }

        @Override
        public void close() {

        }
    }
}


