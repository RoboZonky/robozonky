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

package com.github.robozonky.app.configuration.daemon;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class BlockedAmountsTest extends AbstractZonkyLeveragingTest {

    private final BlockedAmount BA1 = new BlockedAmount(1, BigDecimal.valueOf(200)),
            BA2 = new BlockedAmount(2, BigDecimal.valueOf(1000)),
            BA3 = new BlockedAmount(3, BigDecimal.valueOf(2000));

    @Test
    public void newBlockedAmounts() {
        final Zonky zonky = harmlessZonky(10_000);
        Mockito.when(zonky.getBlockedAmounts()).thenReturn(Stream.of(BA1, BA2));
        final Authenticated auth = mockAuthentication(zonky);
        final Portfolio p = Mockito.mock(Portfolio.class);
        final BlockedAmounts blockedAmounts = new BlockedAmounts();
        // verify the first two blocked amounts were registered
        blockedAmounts.accept(p, auth);
        Mockito.verify(p).newBlockedAmount(ArgumentMatchers.eq(zonky), ArgumentMatchers.eq(BA1));
        Mockito.verify(p).newBlockedAmount(ArgumentMatchers.eq(zonky), ArgumentMatchers.eq(BA2));
        // verify only the new blocked amount was registered, and that it only happened the first time
        Mockito.reset(p);
        Mockito.when(zonky.getBlockedAmounts())
                .thenAnswer((Answer<Stream<BlockedAmount>>) invocation -> Stream.of(BA1, BA2, BA3));
        blockedAmounts.accept(p, auth);
        blockedAmounts.accept(p, auth);
        Mockito.verify(p, Mockito.times(1))
                .newBlockedAmount(ArgumentMatchers.eq(zonky), ArgumentMatchers.eq(BA3));
    }
}
