/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.management;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;

import com.github.robozonky.app.portfolio.Delinquent;
import com.github.robozonky.app.portfolio.Delinquents;
import com.github.robozonky.common.AbstractStateLeveragingTest;
import com.github.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class DelinquencyTest extends AbstractStateLeveragingTest {

    private final OffsetDateTime EPOCH = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);

    @Test
    public void empty() {
        final Delinquency d = new Delinquency();
        Assertions.assertThat(d.getLatestUpdatedDateTime()).isEqualTo(EPOCH);
        Assertions.assertThat(d.getAll()).isEmpty();
        Assertions.assertThat(d.get10Plus()).isEmpty();
        Assertions.assertThat(d.get30Plus()).isEmpty();
        Assertions.assertThat(d.get60Plus()).isEmpty();
        Assertions.assertThat(d.get90Plus()).isEmpty();
    }

    @Test
    public void something() {
        final Delinquents mock = Mockito.mock(Delinquents.class);
        final Delinquent delinquent = new Delinquent(1, EPOCH.toLocalDate());
        Mockito.when(mock.getLastUpdateTimestamp()).thenReturn(OffsetDateTime.now());
        Mockito.when(mock.getDelinquents()).thenReturn(Collections.singletonList(delinquent));
        final Delinquency d = new Delinquency(mock);
        Assertions.assertThat(d.getLatestUpdatedDateTime()).isBeforeOrEqualTo(OffsetDateTime.now());
        Assertions.assertThat(d.getAll()).containsOnlyKeys(delinquent.getLoanId());
        Assertions.assertThat(d.get10Plus()).containsOnlyKeys(delinquent.getLoanId());
        Assertions.assertThat(d.get30Plus()).containsOnlyKeys(delinquent.getLoanId());
        Assertions.assertThat(d.get60Plus()).containsOnlyKeys(delinquent.getLoanId());
        Assertions.assertThat(d.get90Plus()).containsOnlyKeys(delinquent.getLoanId());
    }
}

