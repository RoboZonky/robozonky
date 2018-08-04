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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Transactions {

    private static final Logger LOGGER = LoggerFactory.getLogger(Transaction.class);

    private final OffsetDateTime currentEpoch;
    private final OffsetDateTime minimalEpoch;
    private final Map<SourceAgnosticTransaction, OffsetDateTime> transactionDetectionTimestamps;

    Transactions() {
        this(OffsetDateTime.now());
    }

    Transactions(final OffsetDateTime lastZonkyUpdate) {
        this(lastZonkyUpdate, Collections.emptyMap());
    }

    private Transactions(final OffsetDateTime newLastZonkyUpdate,
                         final Map<SourceAgnosticTransaction, OffsetDateTime> oldTransactions) {
        this.currentEpoch = newLastZonkyUpdate;
        this.minimalEpoch = currentEpoch.minusDays(7); // lazy payers are back-dated, we would like to catch them
        this.transactionDetectionTimestamps = oldTransactions.entrySet().stream()
                .filter(e -> e.getValue().isAfter(minimalEpoch))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new IllegalStateException("Merging " + a + " and " + b + " should not be necessary.");
                }, LinkedHashMap::new));
    }

    boolean fromZonky(final Transaction transaction) {
        return addReal(SourceAgnosticTransaction.real(transaction));
    }

    private boolean addReal(final SourceAgnosticTransaction transaction) {
        final Optional<SourceAgnosticTransaction> original = transactionDetectionTimestamps.keySet().stream()
                .filter(e -> Objects.equals(e, transaction))
                .findFirst();
        if (original.isPresent()) {
            original.get().promote(transaction.getSource());
            return false;
        } else {
            LOGGER.debug("Adding real transaction into currentEpoch '{}': {}.", currentEpoch, transaction);
            transactionDetectionTimestamps.put(transaction, currentEpoch.plusSeconds(1));
            return true;
        }
    }

    boolean fromZonky(final BlockedAmount blockedAmount) {
        return addReal(SourceAgnosticTransaction.blockation(blockedAmount));
    }

    public Stream<SourceAgnosticTransaction> getUnprocessed() {
        return getUnprocessed(currentEpoch);
    }

    public Stream<SourceAgnosticTransaction> getUnprocessed(final OffsetDateTime zonkyUpdatedOn) {
        return transactionDetectionTimestamps.entrySet().stream()
                .filter(e -> e.getKey().getSource() == TransactionSource.SYNTHETIC ||
                        e.getKey().getSource() == TransactionSource.BLOCKED_AMOUNT ||
                        e.getValue().isAfter(zonkyUpdatedOn))
                .map(Map.Entry::getKey);
    }

    boolean fromInvestment(final int loanId, final BigDecimal amount) {
        final SourceAgnosticTransaction t = SourceAgnosticTransaction.synthetic(OffsetDateTime.now(), loanId,
                                                                                TransactionOrientation.OUT,
                                                                                TransactionCategory.INVESTMENT, amount);
        return addSynthetic(t);
    }

    private boolean addSynthetic(final SourceAgnosticTransaction synthetic) {
        if (transactionDetectionTimestamps.containsKey(synthetic)) {
            LOGGER.debug("Duplicate synthetic transaction: {}.", synthetic);
            return false;
        }
        LOGGER.debug("Adding synthetic transaction into currentEpoch '{}': {}.", currentEpoch, synthetic);
        transactionDetectionTimestamps.put(synthetic, OffsetDateTime.now());
        return true;
    }

    public OffsetDateTime getCurrentEpoch() {
        return currentEpoch;
    }

    public OffsetDateTime getMinimalEpoch() {
        return minimalEpoch;
    }

    boolean fromPurchase(final int loanId, final BigDecimal amount) {
        final SourceAgnosticTransaction t = SourceAgnosticTransaction.synthetic(OffsetDateTime.now(), loanId,
                                                                                TransactionOrientation.OUT,
                                                                                TransactionCategory.SMP_BUY, amount);
        return addSynthetic(t);
    }

    Transactions rebase(final OffsetDateTime zonkyUpdatedOn) {
        LOGGER.debug("Rebasing transactions to currentEpoch '{}'.", zonkyUpdatedOn);
        return new Transactions(zonkyUpdatedOn, transactionDetectionTimestamps);
    }
}
