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

package com.github.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

class TransactionLogTest extends AbstractZonkyLeveragingTest {

    private static final Random RANDOM = new Random();

    @Test
    void fresh() {
        final TransactionLog t = new TransactionLog();
        assertSoftly(softly -> {
            softly.assertThat(t.getAdjustments()).isEmpty();
            softly.assertThat(t.getSynthetics()).isEmpty();
        });
    }

    /**
     * When carrying over synthetics, we need to make sure that they are properly accounted for among the adjustments.
     * Otherwise dry run would show incorrect rating shares.
     */
    @Test
    void startsWithSynthetics() {
        final Loan l1 = Loan.custom().setId(1).setRating(Rating.A).build();
        final Loan l2 = Loan.custom().setId(2).setRating(Rating.A).build();
        final Loan l3 = Loan.custom().setId(3).setRating(Rating.D).build();
        final Zonky z = harmlessZonky(10_000);
        final Tenant t = mockTenant(z);
        final Collection<Synthetic> preexisting = Stream.of(l1, l2, l3)
                .peek(l -> when(z.getLoan(eq(l.getId()))).thenReturn(l))
                .map(l -> new Synthetic(l.getId(), BigDecimal.TEN))
                .collect(Collectors.toList());
        final TransactionLog l = new TransactionLog(t, preexisting);
        assertSoftly(softly -> {
            softly.assertThat(l.getAdjustments()).containsOnlyKeys(Rating.A, Rating.D);
            softly.assertThat(l.getAdjustments().get(Rating.A)).isEqualTo(BigDecimal.TEN.add(BigDecimal.TEN));
            softly.assertThat(l.getAdjustments().get(Rating.D)).isEqualTo(BigDecimal.TEN);
            softly.assertThat(l.getSynthetics()).containsOnly(preexisting.toArray(new Synthetic[preexisting.size()]));
        });
    }

    @Test
    void processingTransactions() {
        final Loan l0 = Loan.custom().setId(RANDOM.nextInt()).setRating(Rating.D).build();
        final Transaction boughtPreviously = new Transaction(1, l0, BigDecimal.TEN, TransactionCategory.SMP_BUY,
                                                             TransactionOrientation.OUT);
        final Synthetic preexisting = new Synthetic(l0.getId(), boughtPreviously.getAmount());
        final Loan l1 = Loan.custom().setId(RANDOM.nextInt()).setRating(Rating.C).build();
        final Transaction bought = new Transaction(2, l1, BigDecimal.TEN, TransactionCategory.SMP_BUY,
                                                   TransactionOrientation.OUT);
        final Loan l2 = Loan.custom().setId(RANDOM.nextInt()).setRating(Rating.B).build();
        final Transaction sold = new Transaction(3, l2, BigDecimal.TEN, TransactionCategory.SMP_SELL,
                                                 TransactionOrientation.IN);
        final Loan l3 = Loan.custom().setId(RANDOM.nextInt()).setRating(Rating.A).build();
        final Transaction ignoredCategory = new Transaction(4, l3, BigDecimal.TEN, TransactionCategory.INVESTMENT,
                                                            TransactionOrientation.IN);
        final Statistics statistics = Statistics.empty();
        final OffsetDateTime timestamp = statistics.getTimestamp();
        final Zonky z = harmlessZonky(10_000);
        when(z.getTransactions(
                (Select) argThat(select -> { // only retrieve the transactions when the proper filter is used
            final Select select2 = Select.unrestricted()
                    .greaterThanOrEquals("transaction.transactionDate", timestamp.toLocalDate());
            return Objects.equals(select, select2);
        }))).thenReturn(Stream.of(boughtPreviously, bought, ignoredCategory, sold));
        Stream.of(l0, l1, l2, l3).forEach(l -> when(z.getLoan(eq(l.getId()))).thenReturn(l));
        final Tenant t = mockTenant(z);
        final TransactionLog tl = new TransactionLog(t, Collections.singleton(preexisting));
        // the test starts here
        final Set<Integer> soldLoans = tl.update(statistics, t);
        assertThat(soldLoans).containsOnly(sold.getLoanId()); // transaction correctly identified as sold
        assertThat(tl.getAdjustments()).containsOnlyKeys(Rating.B, Rating.C, Rating.D);
        assertThat(tl.getAdjustments().get(Rating.B)).isEqualTo(sold.getAmount().negate()); // adjust for the sale
        assertThat(tl.getAdjustments().get(Rating.C)).isEqualTo(bought.getAmount()); // adjust for the buy
        // retain the buy from some previous session
        assertThat(tl.getAdjustments().get(Rating.D)).isEqualTo(boughtPreviously.getAmount());
        assertThat(tl.getSynthetics()).isEmpty(); // the synthetic was removed due to boughtPreviously transaction
    }

    @Test
    void processingBlockedAmounts() {
        final Loan l0 = Loan.custom().setId(RANDOM.nextInt()).setRating(Rating.D).build();
        final BlockedAmount blockedPreviously = new BlockedAmount(l0.getId(), BigDecimal.TEN);
        final Loan l1 = Loan.custom().setId(RANDOM.nextInt()).setRating(Rating.C).build();
        final BlockedAmount blocked = new BlockedAmount(l1.getId(), BigDecimal.TEN);
        final Loan l2 = Loan.custom().setId(RANDOM.nextInt()).setRating(Rating.A).build();
        final BlockedAmount ignoredCategory = new BlockedAmount(l2.getId(), BigDecimal.TEN,
                                                                TransactionCategory.WITHDRAW);
        final Synthetic preexisting = new Synthetic(l0.getId(), blockedPreviously.getAmount());
        final Statistics statistics = Statistics.empty();
        final Zonky z = harmlessZonky(10_000);
        when(z.getBlockedAmounts()).thenReturn(Stream.of(blockedPreviously, blocked, ignoredCategory));
        Stream.of(l0, l1, l2).forEach(l -> when(z.getLoan(eq(l.getId()))).thenReturn(l));
        final Tenant t = mockTenant(z);
        final TransactionLog tl = new TransactionLog(t, Collections.singleton(preexisting));
        // the test starts here
        final Set<Integer> soldLoans = tl.update(statistics, t);
        assertThat(soldLoans).isEmpty(); // blocked amounts are only concerned with new investments
        assertThat(tl.getAdjustments()).containsOnlyKeys(Rating.C, Rating.D);
        assertThat(tl.getAdjustments().get(Rating.C)).isEqualTo(blocked.getAmount());
        assertThat(tl.getAdjustments().get(Rating.D)).isEqualTo(blockedPreviously.getAmount());
        assertThat(tl.getSynthetics()).isEmpty(); // the synthetic was removed due to blockedPreviously transaction
    }
}
