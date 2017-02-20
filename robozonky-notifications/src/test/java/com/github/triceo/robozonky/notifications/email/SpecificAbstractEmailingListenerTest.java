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

import java.util.Map;

import com.github.triceo.robozonky.api.notifications.Event;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class SpecificAbstractEmailingListenerTest {

    @Test
    public void failedTemplate() {
        final AbstractEmailingListener<Event> l = new AbstractEmailingListener<Event>(null) {
            @Override
            boolean shouldSendEmail(final Event event) {
                return true;
            }

            @Override
            String getSubject(final Event event) {
                return "";
            }

            @Override
            String getTemplateFileName() {
                return null;
            }

            @Override
            Map<String, Object> getData(final Event event) {
                return null;
            }
        };
        Assertions.assertThatThrownBy(() -> l.handle(Mockito.mock(Event.class))).isInstanceOf(RuntimeException.class);
    }

}
