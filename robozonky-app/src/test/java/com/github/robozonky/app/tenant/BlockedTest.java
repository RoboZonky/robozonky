/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.app.tenant;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.test.AbstractRoboZonkyTest;

class BlockedTest extends AbstractRoboZonkyTest {

    @Test
    void fromBigDecimal() {
        final Blocked b = new Blocked(1, Money.from(10), Rating.D);
        assertSoftly(softly -> {
            softly.assertThat(b.getAmount())
                .isEqualTo(Money.from(10));
            softly.assertThat(b.getRating())
                .isEqualTo(Rating.D);
            softly.assertThat(b.getId())
                .isEqualTo(1);
        });
    }

    @Test
    void nonPersistentExpiring() {
        final Zonky zonky = harmlessZonky();
        final Tenant tenant = mockTenant(zonky);
        OffsetDateTime loadOn = zonky.getStatistics()
            .getTimestamp()
            .minusSeconds(1);
        DateUtil.setSystemClock(Clock.fixed(loadOn.toInstant(), Defaults.ZONE_ID));
        final Blocked b = new Blocked(0, Money.ZERO, Rating.D, false);
        RemoteData r = RemoteData.load(tenant);
        assertThat(b.isValid(r)).isFalse();
    }

    @Test
    void equals() {
        final Blocked b = new Blocked(2, Money.from(10), Rating.D);
        assertThat(b).isEqualTo(b);
        assertThat(b).isNotEqualTo(null);
        assertThat(b).isNotEqualTo("");
        final Blocked sameB = new Blocked(2, Money.from(10), Rating.D);
        assertThat(sameB).isEqualTo(b);
        final Blocked differentB = new Blocked(2, Money.from(1), Rating.D);
        assertThat(differentB).isNotEqualTo(b);
    }
}
