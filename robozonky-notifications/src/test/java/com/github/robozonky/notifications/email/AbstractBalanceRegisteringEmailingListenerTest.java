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

package com.github.robozonky.notifications.email;

import java.io.IOException;
import java.util.Properties;
import java.util.function.Function;

import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AbstractBalanceRegisteringEmailingListenerTest extends AbstractRoboZonkyTest {

    private static final int RESULT = 123456;
    private static final Function<RoboZonkyTestingEvent, Integer> F = (e) -> RESULT;

    @Test
    void checkBalance() throws IOException {
        final Properties props = new Properties();
        props.load(NotificationPropertiesTest.class.getResourceAsStream("notifications-enabled.cfg"));
        final ListenerSpecificNotificationProperties p =
                new ListenerSpecificNotificationProperties(SupportedListener.TESTING,
                                                           new NotificationProperties(props));
        final TestingEmailingListener t = new TestingEmailingListener(p);
        assertThat(t.getBalance()).isEqualTo(RESULT);
        assertThat(t.countFinishers()).isEqualTo(2); // the core finisher and the balance finisher
    }

    private static final class TestingEmailingListener extends
                                                       AbstractBalanceRegisteringEmailingListener<RoboZonkyTestingEvent> {

        TestingEmailingListener(final ListenerSpecificNotificationProperties properties) {
            super(F, properties);
        }

        @Override
        String getSubject(final RoboZonkyTestingEvent event) {
            return "No actual subject";
        }

        @Override
        String getTemplateFileName() {
            return "testing.ftl";
        }

        int getBalance() {
            return this.getNewBalance(null);
        }
    }
}
