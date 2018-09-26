/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ValueTrackerTest {

    @SuppressWarnings("unchecked")
    @Test
    void propagatesChanges() {
        final Consumer<BigDecimal> listener = mock(Consumer.class);
        final ValueTracker t = new ValueTracker(BigDecimal.TEN, listener);
        assertThat(t.get()).isEqualTo(BigDecimal.TEN);
        t.set(BigDecimal.ONE);
        assertThat(t.get()).isEqualTo(BigDecimal.ONE);
        verify(listener).accept(eq(BigDecimal.ONE));
    }

}
