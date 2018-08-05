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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Transfers {

    private static final Logger LOGGER = LoggerFactory.getLogger(Transaction.class);

    private final OffsetDateTime currentEpoch;
    private final OffsetDateTime minimalEpoch;
    private final Map<SourceAgnosticTransfer, OffsetDateTime> transferDetectionTimestamps;

    Transfers(final OffsetDateTime lastZonkyUpdate) {
        this(lastZonkyUpdate, Collections.emptyMap());
    }

    private Transfers(final OffsetDateTime newLastZonkyUpdate,
                      final Map<SourceAgnosticTransfer, OffsetDateTime> oldTransfers) {
        this.currentEpoch = newLastZonkyUpdate;
        this.minimalEpoch = currentEpoch.minusDays(7); // lazy payers are back-dated, we would like to catch them
        this.transferDetectionTimestamps = oldTransfers.entrySet().stream()
                .filter(e -> e.getValue().isAfter(minimalEpoch))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new IllegalStateException("Merging " + a + " and " + b + " should not be necessary.");
                }, LinkedHashMap::new));
    }

    boolean fromZonky(final Transaction transaction, final Supplier<Rating> ratingSupplier) {
        return addReal(SourceAgnosticTransfer.real(transaction, ratingSupplier));
    }

    private boolean addReal(final SourceAgnosticTransfer transfer) {
        final Optional<SourceAgnosticTransfer> original = transferDetectionTimestamps.keySet().stream()
                .filter(e -> Objects.equals(e, transfer))
                .findFirst();
        if (original.isPresent()) {
            original.get().promote(transfer.getSource());
            return false;
        } else {
            LOGGER.debug("Adding real transfer into currentEpoch '{}': {}.", currentEpoch, transfer);
            transferDetectionTimestamps.put(transfer, currentEpoch.plusSeconds(1));
            return true;
        }
    }

    boolean fromZonky(final BlockedAmount blockedAmount, final Supplier<Rating> ratingSupplier) {
        return addReal(SourceAgnosticTransfer.blockation(blockedAmount, ratingSupplier));
    }

    public Stream<SourceAgnosticTransfer> getUnprocessed() {
        return getUnprocessed(currentEpoch);
    }

    public Stream<SourceAgnosticTransfer> getUnprocessed(final OffsetDateTime zonkyUpdatedOn) {
        return transferDetectionTimestamps.entrySet().stream()
                .filter(e -> e.getKey().getSource() == TransferSource.SYNTHETIC ||
                        e.getKey().getSource() == TransferSource.BLOCKED_AMOUNT ||
                        e.getValue().isAfter(zonkyUpdatedOn))
                .map(Map.Entry::getKey);
    }

    boolean fromInvestment(final int loanId, final Rating rating, final BigDecimal amount) {
        final SourceAgnosticTransfer t = SourceAgnosticTransfer.synthetic(OffsetDateTime.now(), loanId,
                                                                          TransactionOrientation.OUT,
                                                                          TransactionCategory.INVESTMENT, amount,
                                                                          rating);
        return addSynthetic(t);
    }

    private boolean addSynthetic(final SourceAgnosticTransfer transfer) {
        if (transferDetectionTimestamps.containsKey(transfer)) {
            LOGGER.debug("Duplicate synthetic transfer: {}.", transfer);
            return false;
        }
        LOGGER.debug("Adding synthetic transfer into currentEpoch '{}': {}.", currentEpoch, transfer);
        transferDetectionTimestamps.put(transfer, OffsetDateTime.now());
        return true;
    }

    public OffsetDateTime getCurrentEpoch() {
        return currentEpoch;
    }

    public OffsetDateTime getMinimalEpoch() {
        return minimalEpoch;
    }

    boolean fromPurchase(final int loanId, final Rating rating, final BigDecimal amount) {
        final SourceAgnosticTransfer t = SourceAgnosticTransfer.synthetic(OffsetDateTime.now(), loanId,
                                                                          TransactionOrientation.OUT,
                                                                          TransactionCategory.SMP_BUY, amount,
                                                                          rating);
        return addSynthetic(t);
    }

    Transfers rebase(final OffsetDateTime zonkyUpdatedOn) {
        LOGGER.debug("Rebasing transfers to currentEpoch '{}'.", zonkyUpdatedOn);
        return new Transfers(zonkyUpdatedOn, transferDetectionTimestamps);
    }
}
