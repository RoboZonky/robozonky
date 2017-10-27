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
import java.util.function.Consumer;

import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.api.notifications.SessionInfo;
import com.github.robozonky.common.AbstractStateLeveragingTest;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AbstractEmailingListenerTest extends AbstractStateLeveragingTest {

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);
    private final RoboZonkyTestingEvent EVENT = new RoboZonkyTestingEvent();

    @Test
    public void spamProtectionAvailable() throws IOException {
        final Properties props = new Properties();
        props.load(NotificationPropertiesTest.class.getResourceAsStream("notifications-enabled.cfg"));
        int sendCount = 1;
        props.setProperty("hourlyMaxEmails", String.valueOf(sendCount)); // spam protection
        final ListenerSpecificNotificationProperties p =
                new ListenerSpecificNotificationProperties(SupportedListener.TESTING,
                                                           new NotificationProperties(props));
        final Consumer<RoboZonkyTestingEvent> c = Mockito.mock(Consumer.class);
        final TestingEmailingListener l = new TestingEmailingListener(p);
        l.registerFinisher(c);
        Assertions.assertThat(l.countFinishers()).isEqualTo(2); // both spam protection and custom finisher available
        l.handle(EVENT, new SessionInfo("someone@somewhere.net"));
        Mockito.verify(c, Mockito.times(sendCount)).accept(ArgumentMatchers.any());
        Assertions.assertThat(greenMail.getReceivedMessages()).hasSize(sendCount);
        l.handle(EVENT, new SessionInfo("someone@somewhere.net"));
        // e-mail not re-sent, finisher not called again
        Mockito.verify(c, Mockito.times(sendCount)).accept(ArgumentMatchers.any());
        Assertions.assertThat(greenMail.getReceivedMessages()).hasSize(sendCount);
    }

    private static final class TestingEmailingListener extends AbstractEmailingListener<RoboZonkyTestingEvent> {

        public TestingEmailingListener(final ListenerSpecificNotificationProperties properties) {
            super(properties);
        }

        @Override
        String getSubject(final RoboZonkyTestingEvent event) {
            return "No actual subject";
        }

        @Override
        String getTemplateFileName() {
            return "testing.ftl";
        }
    }
}
