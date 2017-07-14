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

package com.github.triceo.robozonky.notifications;

import java.io.IOException;
import java.util.Properties;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class NotificationPropertiesTest {

    private static final class TestingProperties extends NotificationProperties {

        TestingProperties(final Properties source) {
            super(source);
        }

        @Override
        protected int getGlobalHourlyLimit() {
            return Integer.MAX_VALUE;
        }

    }

    @Test
    public void equals() throws IOException {
        final Properties p = new Properties();
        p.load(NotificationPropertiesTest.class.getResourceAsStream("email/notifications-enabled.cfg"));
        final NotificationPropertiesTest.TestingProperties tp = new NotificationPropertiesTest.TestingProperties(p);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(tp).isEqualTo(tp);
            softly.assertThat(tp.hashCode()).isEqualTo(tp.properties.hashCode());
        });
    }

    @Test
    public void notEquals() throws IOException {
        final Properties p = new Properties();
        p.load(NotificationPropertiesTest.class.getResourceAsStream("email/notifications-enabled.cfg"));
        final NotificationPropertiesTest.TestingProperties tp = new NotificationPropertiesTest.TestingProperties(p);
        final Properties p2 = new Properties();
        p2.load(NotificationPropertiesTest.class.getResourceAsStream("files/notifications-enabled.cfg"));
        final NotificationPropertiesTest.TestingProperties tp2 = new NotificationPropertiesTest.TestingProperties(p2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(tp).isNotEqualTo(tp2);
            softly.assertThat(tp2).isNotEqualTo(tp);
            softly.assertThat(tp).isNotEqualTo(null);
            softly.assertThat(tp).isNotEqualTo("");
        });
    }

}
