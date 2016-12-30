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

package com.github.triceo.robozonky.notifications.files;

import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyStartingEvent;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class FileStoringListenerServiceTest {

    private FileStoringListenerService service = new FileStoringListenerService();

    @Test
    public void supportsInvestmentDelegatedEvent() {
        Assertions.assertThat(service.findListener(InvestmentDelegatedEvent.class)).isPresent();
    }

    @Test
    public void supportsInvestmentRejectedEvent() {
        Assertions.assertThat(service.findListener(InvestmentRejectedEvent.class)).isPresent();
    }

    @Test
    public void supportsInvestmentMadeEvent() {
        Assertions.assertThat(service.findListener(InvestmentMadeEvent.class)).isPresent();
    }

    @Test
    public void doesNotSupportsUnknownEvent() {
        Assertions.assertThat(service.findListener(RoboZonkyStartingEvent.class)).isEmpty();
    }
}
