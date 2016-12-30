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

package com.github.triceo.robozonky.notifications.email;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

public class EmailListenerServiceTest extends AbstractListenerTest {

    @Rule
    public final ProvideSystemProperty myPropertyHasMyValue = new ProvideSystemProperty(
            NotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
            NotificationPropertiesTest.class.getResource("notifications-enabled.cfg").toString());

    @Test
    public void noPropertiesNoListeners() {
        System.setProperty(NotificationProperties.CONFIG_FILE_LOCATION_PROPERTY, "");
        final EmailListenerService e = new EmailListenerService();
        Assertions.assertThat(e.findListener(this.event.getClass())).isEmpty();
    }

    @Test
    public void reportingEnabledHaveListeners() {
        final EmailListenerService e = new EmailListenerService();
        Assertions.assertThat(e.findListener(this.event.getClass())).isPresent();
    }

}
