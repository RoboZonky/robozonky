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

package com.github.robozonky.app.events;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.events.impl.EventFactory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalEventsTest extends AbstractZonkyLeveragingTest {

    @Test
    void fireReturnsFuture() {
        final CompletableFuture<?> result = GlobalEvents.get().fire(EventFactory.roboZonkyEnding());
        result.join(); // make sure it does not throw
        assertThat(getEventsRequested()).hasSize(1);
    }
}
