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

package com.github.robozonky.app.runtime;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.github.robozonky.app.AbstractEventLeveragingTest;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeTest extends AbstractEventLeveragingTest {

    @Test
    void values() {
        final Lifecycle l = mock(Lifecycle.class);
        when(l.getZonkyApiLastUpdate()).thenReturn(OffsetDateTime.now());
        when(l.getZonkyApiVersion()).thenReturn(Optional.of("1.0.0"));
        final RuntimeMBean r = new Runtime(l);
        assertSoftly(softly -> {
            softly.assertThat(r.getVersion()).isEqualTo(Defaults.ROBOZONKY_VERSION);
            softly.assertThat(r.getZonkyApiVersion()).isEqualTo("1.0.0");
            softly.assertThat(r.getLastUpdated()).isBeforeOrEqualTo(OffsetDateTime.now());
        });
        r.stopDaemon();
        verify(l).resumeToShutdown();
    }

}
