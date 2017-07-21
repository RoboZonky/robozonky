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

package com.github.triceo.robozonky.notifications.email;

import javax.mail.internet.MimeMessage;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.SessionInfo;
import com.github.triceo.robozonky.internal.api.Defaults;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

public class EmailingListenerTest extends AbstractEmailingListenerTest {

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Test
    public void testMailSent() throws Exception {
        final AbstractEmailingListener<Event> l = this.getEmailingListener();
        Assertions.assertThat(this.event).isInstanceOf(this.listenerType.getEventType());
        l.handle(this.event, new SessionInfo("someone@somewhere.net"));
        Assertions.assertThat(l.getData(this.event)).isNotNull();
        Assertions.assertThat(l.shouldSendEmail(this.event)).isTrue();
        Assertions.assertThat(greenMail.getReceivedMessages()).hasSize(1);
        final MimeMessage m = greenMail.getReceivedMessages()[0];
        Assertions.assertThat(m.getContentType()).contains(Defaults.CHARSET.displayName());
        Assertions.assertThat(m.getSubject()).isNotNull().isEqualTo(l.getSubject(this.event));
        Assertions.assertThat(m.getFrom()[0].toString()).contains("user@seznam.cz");
    }
}
