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
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.ws.rs.NotFoundException;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.util.Maps;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class BlockedAmountProcessorTest extends AbstractZonkyLeveragingTest {

    private final Zonky zonky = harmlessZonky(10_000);
    private final Tenant tenant = mockTenant(zonky);

    @Test
    void ignoresBlockedAmountsFromZonky() {
        final BlockedAmount fee = new BlockedAmount(BigDecimal.ONE);
        final BlockedAmount forLoan = new BlockedAmount(1, BigDecimal.TEN);
        final Loan l = Loan.custom().setId(1).setRating(Rating.D).build();
        when(zonky.getBlockedAmounts()).thenAnswer(i -> Stream.of(fee, forLoan));
        when(zonky.getLoan(eq(l.getId()))).thenReturn(l);
        final BlockedAmountProcessor p = BlockedAmountProcessor.create(tenant);
        assertThat(p.getAdjustments()).containsExactly(Maps.entry(Rating.D, BigDecimal.TEN));
    }

    @Test
    void handles404thrownByZonky() {
        final BlockedAmount fee = new BlockedAmount(BigDecimal.ONE);
        final BlockedAmount forLoan = new BlockedAmount(1, BigDecimal.TEN);
        when(zonky.getBlockedAmounts()).thenAnswer(i -> Stream.of(fee, forLoan));
        doThrow(new NotFoundException()).when(zonky).getLoan(anyInt());
        assertThatThrownBy(() -> BlockedAmountProcessor.create(tenant))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(NotFoundException.class);
    }

    @Test
    void updatesCorrectly() {
        final Supplier<BlockedAmountProcessor> p = BlockedAmountProcessor.createLazy(tenant); // empty
        final BlockedAmount fee = new BlockedAmount(BigDecimal.ONE);
        final BlockedAmount forLoan = new BlockedAmount(1, BigDecimal.TEN);
        final Loan l = Loan.custom().setId(1).setRating(Rating.D).build();
        when(zonky.getBlockedAmounts()).thenAnswer(i -> Stream.of(fee, forLoan));
        when(zonky.getLoan(eq(l.getId()))).thenReturn(l);
        final Portfolio po = Portfolio.create(tenant, p);
        final TransactionalPortfolio tp = new TransactionalPortfolio(po, tenant);
        final BlockedAmountProcessor bp = p.get();
        bp.accept(tp);
        assertThat(bp.getAdjustments()).containsExactly(Maps.entry(Rating.D, BigDecimal.TEN));
    }

    @Test
    void handlesBlockedAmountLifecycleCorrectly() {
        final Supplier<BlockedAmountProcessor> sp = BlockedAmountProcessor.createLazy(tenant); // empty
        final BlockedAmountProcessor p = sp.get();
        p.simulateCharge(1, Rating.D, BigDecimal.TEN);
        assertThat(p.getAdjustments()).containsExactly(Maps.entry(Rating.D, BigDecimal.TEN));
        // now test that promotion to real blocked amount works properly
        final int loanId = 1;
        final BlockedAmount forLoan = new BlockedAmount(loanId, BigDecimal.TEN);
        final Loan l = Loan.custom().setId(loanId).setRating(Rating.D).build();
        when(zonky.getBlockedAmounts()).thenAnswer(i -> Stream.of(forLoan));
        when(zonky.getLoan(eq(l.getId()))).thenReturn(l);
        final Portfolio po = Portfolio.create(tenant, sp);
        final TransactionalPortfolio tp = new TransactionalPortfolio(po, tenant);
        p.accept(tp); // no change in blocked amounts, just the synthetic is removed in favor of real
        assertThat(p.getAdjustments()).containsExactly(Maps.entry(Rating.D, BigDecimal.TEN));
        // and now remove even the real one as it was finally processed by Zonky
        when(zonky.getBlockedAmounts()).thenAnswer(i -> Stream.empty());
        p.accept(tp);
        assertThat(p.getAdjustments()).isEmpty();
    }
}
