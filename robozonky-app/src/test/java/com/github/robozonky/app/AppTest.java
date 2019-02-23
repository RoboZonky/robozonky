/*
 * Copyright 2019 The RoboZonky Project
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

import java.lang.management.ManagementFactory;
import java.time.OffsetDateTime;
import java.util.List;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.api.notifications.RoboZonkyStartingEvent;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.events.AbstractEventLeveragingTest;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.common.async.Scheduler;
import com.github.robozonky.common.async.Tasks;
import com.github.robozonky.common.management.AbstractBaseMBean;
import com.github.robozonky.common.management.BaseMBean;
import com.github.robozonky.common.management.Management;
import com.github.robozonky.common.management.ManagementBean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
        doNothing().when(main).ensureLiveness(); // avoid going out to actual live Zonky server
        try {
            assertThat(main.execute(new MyInvestmentMode())).isEqualTo(ReturnCode.OK);
        } finally { // clean up, shutting down executors etc.
            main.exit(new ShutdownHook.Result(ReturnCode.OK));
        }
        verify(main).ensureLiveness();
        verify(main).actuallyExit(ReturnCode.OK.getCode());
        final List<Event> events = getEventsRequested();
        assertThat(events).hasSize(3);
        assertSoftly(softly -> {
            softly.assertThat(events.get(0)).isInstanceOf(RoboZonkyStartingEvent.class);
            softly.assertThat(events.get(1)).isInstanceOf(RoboZonkyInitializedEvent.class);
            softly.assertThat(events.get(2)).isInstanceOf(RoboZonkyEndingEvent.class);
        });
    }

    @Test
    void unregistersBeans() {
        final App main = spy(new App());
        doNothing().when(main).actuallyExit(anyInt());
        doNothing().when(main).ensureLiveness(); // avoid going out to actual live Zonky server
        final int countBefore = ManagementFactory.getPlatformMBeanServer().getMBeanCount();
        main.execute(new MyInvestmentMode());
        final int countAfter = ManagementFactory.getPlatformMBeanServer().getMBeanCount();
        assertThat(countAfter).isGreaterThan(countBefore);
        main.exit(new ShutdownHook.Result(ReturnCode.OK));
        final int finalCount = ManagementFactory.getPlatformMBeanServer().getMBeanCount();
        assertThat(finalCount).isEqualTo(countBefore);
    }

    /**
     * This is a hackish white-box test that tests various internals of the app implementation.
     */
    @Test
    void failsCorrectly() {
        final App main = spy(new App());
        doNothing().when(main).actuallyExit(anyInt());
        doNothing().when(main).ensureLiveness(); // avoid going out to actual live Zonky server
        try {
            final ReturnCode result = main.execute(new MyFailingInvestmentMode());
            assertThat(result).isEqualTo(ReturnCode.ERROR_UNEXPECTED);
        } finally { // clean up, shutting down executors etc.
            final Scheduler s = Tasks.BACKGROUND.scheduler();
            main.exit(new ShutdownHook.Result(ReturnCode.ERROR_UNEXPECTED));
            assertThat(s.isClosed()).isTrue();
        }
    }

    private static class MyInvestmentMode implements InvestmentMode {

        @Override
        public SessionInfo getSessionInfo() {
            return SESSION;
        }

        @Override
        public ReturnCode apply(final Lifecycle lifecycle) {
            final ManagementBean<BaseMBean> mb = new ManagementBean<>(BaseMBean.class, Base::new);
            Management.register(mb);
            return ReturnCode.OK;
        }

        @Override
        public void close() {

        }
    }

    private static final class Base extends AbstractBaseMBean {

        @Override
        public OffsetDateTime getLastUpdated() {
            return OffsetDateTime.now();
        }
    }

    private static class MyFailingInvestmentMode implements InvestmentMode {

        @Override
        public SessionInfo getSessionInfo() {
            return SESSION;
        }

        @Override
        public ReturnCode apply(final Lifecycle lifecycle) {
            throw new IllegalStateException("Testing failure");
        }

        @Override
        public void close() {

        }
    }
}

