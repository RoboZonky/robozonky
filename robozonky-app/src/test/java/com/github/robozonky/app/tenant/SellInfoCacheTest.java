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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.SellFee;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.entities.SellPriceInfo;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SellInfoCacheTest extends AbstractZonkyLeveragingTest {

    public SellInfo mockSellInfo(final BigDecimal price, final BigDecimal fee) {
        SellFee sellFee = mock(SellFee.class);
        when(sellFee.getValue()).thenReturn(Money.from(fee));
        when(sellFee.getExpiresAt()).thenReturn(Optional.of(OffsetDateTime.now()));
        SellPriceInfo sellPriceInfo = mock(SellPriceInfo.class);
        when(sellPriceInfo.getFee()).thenReturn(sellFee);
        SellInfo sellInfo = mock(SellInfo.class);
        when(sellInfo.getSellPrice()).thenReturn(Money.from(price));
        when(sellInfo.getPriceInfo()).thenReturn(sellPriceInfo);
        return sellInfo;
    }

    @Test
    void emptyGetLoan() {
        final long id = 1;
        final SellInfo sellInfo = mockSellInfo(BigDecimal.TEN, BigDecimal.ONE);
        final Zonky z = harmlessZonky();
        final Tenant t = mockTenant(z);
        final Cache<SellInfo> c = Cache.forSellInfo(t);
        assertThat(c.getFromCache(id)).isEmpty(); // nothing returned at first
        when(z.getSellInfo(eq(id))).thenReturn(sellInfo);
        assertThat(c.get(id)).isEqualTo(sellInfo); // return the freshly retrieved loan
    }

    @Test
    void loadLoan() {
        final Instant now = Instant.now();
        final SellInfo sellInfo = mockSellInfo(BigDecimal.TEN, BigDecimal.ONE);
        final Instant instant = Instant.EPOCH;
        setClock(Clock.fixed(instant, Defaults.ZONE_ID));
        final long id = 2;
        final Zonky z = harmlessZonky();
        when(z.getSellInfo(eq(id))).thenReturn(sellInfo);
        final Tenant t = mockTenant(z);
        final Cache<SellInfo> c = Cache.forSellInfo(t);
        assertThat(c.get(id)).isEqualTo(sellInfo); // return the freshly retrieved loan
        verify(z).getSellInfo(eq(id));
        assertThat(c.getFromCache(id)).contains(sellInfo);
        verify(z, times(1)).getSellInfo(eq(id));
        // and now test eviction
        setClock(Clock.fixed(now.plus(Duration.ofHours(2)), Defaults.ZONE_ID));
        assertThat(c.getFromCache(id)).isEmpty();
    }

    @Test
    void fail() {
        final Instant instant = Instant.now();
        setClock(Clock.fixed(instant, Defaults.ZONE_ID));
        final long id = 3;
        final Zonky z = harmlessZonky();
        doThrow(IllegalStateException.class).when(z).getSellInfo(eq(id));
        final Tenant t = mockTenant(z);
        final Cache<SellInfo> c = Cache.forSellInfo(t);
        assertThatThrownBy(() -> c.get(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SellInfo")
                .hasMessageContaining(String.valueOf(id));
    }
}
