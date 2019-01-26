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

package com.github.robozonky.notifications.listeners;

import java.io.IOException;
import java.net.SocketException;
import java.time.OffsetDateTime;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.ConfigStorage;
import com.github.robozonky.notifications.SupportedListener;
import com.github.robozonky.notifications.Target;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RoboZonkyDaemonFailedEventListenerTest extends AbstractRoboZonkyTest {

    private static AbstractTargetHandler getHandler() {
        try {
            final ConfigStorage cs = ConfigStorage.create(AbstractListenerTest.class.getResourceAsStream("notifications-enabled.cfg"));
            return new TestingTargetHandler(cs);
        } catch (final IOException ex) {
            Assertions.fail(ex);
            return null;
        }
    }

    private final AbstractListener<RoboZonkyDaemonFailedEvent> listener =
            new RoboZonkyDaemonFailedEventListener(SupportedListener.DAEMON_FAILED, getHandler());


    @Test
    void networkProblems() {
        final RoboZonkyDaemonFailedEvent event = new RoboZonkyDaemonFailedEvent() {
            @Override
            public Throwable getCause() {
                return new SocketException();
            }

            @Override
            public OffsetDateTime getCreatedOn() {
                return OffsetDateTime.now();
            }
        };
        final boolean allowed = listener.shouldNotify(event, SESSION);
        assertThat(allowed).isFalse();
    }

    @Test
    void normal() {
        final RoboZonkyDaemonFailedEvent event = new RoboZonkyDaemonFailedEvent() {
            @Override
            public Throwable getCause() {
                return new IllegalArgumentException();
            }

            @Override
            public OffsetDateTime getCreatedOn() {
                return OffsetDateTime.now();
            }
        };
        final boolean allowed = listener.shouldNotify(event, SESSION);
        assertThat(allowed).isTrue();
    }

    private static class TestingTargetHandler extends AbstractTargetHandler {

        public TestingTargetHandler(final ConfigStorage storage) {
            super(storage, Target.EMAIL);
        }

        @Override
        public void send(final SessionInfo sessionInfo, final String subject, final String message,
                         final String fallbackMessage) {

        }
    }


}
