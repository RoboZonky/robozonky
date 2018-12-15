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
import java.util.Collections;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class PortfolioTest extends AbstractZonkyLeveragingTest {

    @Test
    void chargeSimulation() {
        final BlockedAmountProcessor ba = spy(new BlockedAmountProcessor());
        final Portfolio p = Portfolio.create(mockTenant(), () -> ba);
        final PortfolioOverview po = p.getOverview();
        p.simulateCharge(1, Rating.A, BigDecimal.TEN);
        verify(ba).simulateCharge(eq(1), eq(Rating.A), eq(BigDecimal.TEN));
        final PortfolioOverview po2 = p.getOverview();
        assertThat(po2).isNotSameAs(po);
    }

    @Test
    void atRiskUpdate() {
        final BlockedAmountProcessor ba = spy(new BlockedAmountProcessor());
        final Portfolio p = Portfolio.create(mockTenant(), () -> ba);
        final PortfolioOverview po = p.getOverview();
        p.amountsAtRiskUpdated(Collections.emptyMap());
        final PortfolioOverview po2 = p.getOverview();
        assertThat(po2).isSameAs(po); // no change in amounts = no recalculation
        p.amountsAtRiskUpdated(Collections.singletonMap(Rating.A, BigDecimal.TEN));
        final PortfolioOverview po3 = p.getOverview();
        assertThat(po3).isNotSameAs(po2); // no change in amounts = no recalculation
    }

}
