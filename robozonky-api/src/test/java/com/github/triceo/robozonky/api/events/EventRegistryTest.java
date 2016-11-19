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

package com.github.triceo.robozonky.api.events;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class EventRegistryTest {

    @Before
    public void clean() {
        EventRegistry.INSTANCE.clear();
    }

    @Test
    public void nothingRegistered() {
        EventRegistry.fire(new MarketplaceCheckCompleteEvent() {});
    }

    @Test
    public void oneRegistration() {
        // register
        final EventListener<MarketplaceCheckStartedEvent> local = Mockito.mock(EventListener.class);
        Assertions.assertThat(EventRegistry.INSTANCE.addListener(MarketplaceCheckStartedEvent.class, local)).isTrue();
        final EventListener<Event> global = Mockito.mock(EventListener.class);
        Assertions.assertThat(EventRegistry.INSTANCE.addListener(global)).isTrue();
        final MarketplaceCheckStartedEvent e = new MarketplaceCheckStartedEvent() {};
        // fire
        EventRegistry.fire(e);
        Mockito.verify(global, Mockito.times(1)).handle(ArgumentMatchers.eq(e));
        Mockito.verify(local, Mockito.times(1)).handle(ArgumentMatchers.eq(e));
        // deregister
        Assertions.assertThat(EventRegistry.INSTANCE.removeListener(MarketplaceCheckStartedEvent.class, local));
        Assertions.assertThat(EventRegistry.INSTANCE.removeListener(global));
        // fire and check no additional mock invocations were registered
        EventRegistry.fire(e);
        Mockito.verify(global, Mockito.times(1)).handle(ArgumentMatchers.eq(e));
        Mockito.verify(local, Mockito.times(1)).handle(ArgumentMatchers.eq(e));
    }

}
