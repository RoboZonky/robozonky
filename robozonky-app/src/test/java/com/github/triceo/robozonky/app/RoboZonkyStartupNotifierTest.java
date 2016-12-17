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

package com.github.triceo.robozonky.app;

import java.util.Optional;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.events.Event;
import com.github.triceo.robozonky.api.events.EventListener;
import com.github.triceo.robozonky.api.events.EventRegistry;
import com.github.triceo.robozonky.api.events.RoboZonkyEndingEvent;
import com.github.triceo.robozonky.api.events.RoboZonkyInitializedEvent;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.mockito.internal.verification.VerificationModeFactory.*;

public class RoboZonkyStartupNotifierTest {

    @Test
    public void eventsFiring() {
        final EventListener<Event> e = Mockito.mock(EventListener.class);
        EventRegistry.INSTANCE.addListener(e);
        final Optional<Consumer<ReturnCode>> result = new RoboZonkyStartupNotifier().get();
        Mockito.verify(e, times(1)).handle(ArgumentMatchers.any());
        Mockito.verify(e, times(1))
                .handle(ArgumentMatchers.any(RoboZonkyInitializedEvent.class));
        result.get().accept(ReturnCode.OK);
        Mockito.verify(e, times(2)).handle(ArgumentMatchers.any());
        Mockito.verify(e, times(1))
                .handle(ArgumentMatchers.any(RoboZonkyEndingEvent.class));
    }

}
