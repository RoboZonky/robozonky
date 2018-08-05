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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SourceAgnosticTransfer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceAgnosticTransfer.class);
    private final SourceAgnosticTransfer.LoanData loanData;
    private final TransactionCategory category;
    private final BigDecimal amount;
    private final OffsetDateTime dateTime;
    private TransferSource source;

    private SourceAgnosticTransfer(final TransferSource source, final OffsetDateTime dateTime,
                                   final TransactionOrientation orientation, final TransactionCategory category,
                                   final BigDecimal amount, final SourceAgnosticTransfer.LoanData loanData) {
        this(source, dateTime, category, normalizeAmount(orientation, amount), loanData);
    }

    private SourceAgnosticTransfer(final TransferSource source, final OffsetDateTime dateTime,
                                   final TransactionCategory category, final BigDecimal amount,
                                   final SourceAgnosticTransfer.LoanData loanData) {
        this.source = source;
        this.dateTime = dateTime;
        this.category = category;
        this.amount = amount;
        this.loanData = loanData;
    }

    private static BigDecimal normalizeAmount(final TransactionOrientation orientation, final BigDecimal amount) {
        return (orientation == TransactionOrientation.IN) ? amount.abs() : amount.abs().negate();
    }

    public static SourceAgnosticTransfer synthetic(final OffsetDateTime dateTime, final int loanId,
                                                   final TransactionOrientation orientation,
                                                   final TransactionCategory category, final BigDecimal amount,
                                                   final Rating rating) {
        return new SourceAgnosticTransfer(TransferSource.SYNTHETIC, dateTime, orientation, category, amount,
                                          new SourceAgnosticTransfer.LoanData(loanId, () -> rating));
    }

    public static SourceAgnosticTransfer real(final Transaction transaction, final Supplier<Rating> ratingSupplier) {
        final int loanId = transaction.getLoanId();
        final LoanData loanData = loanId == 0 ? null : new LoanData(loanId, ratingSupplier);
        final OffsetDateTime date = transaction.getTransactionDate().atStartOfDay(Defaults.ZONE_ID).toOffsetDateTime();
        return new SourceAgnosticTransfer(TransferSource.REAL, date, transaction.getOrientation(),
                                          transaction.getCategory(), transaction.getAmount(), loanData);
    }

    public static SourceAgnosticTransfer blockation(final BlockedAmount blockedAmount,
                                                    final Supplier<Rating> ratingSupplier) {
        final int loanId = blockedAmount.getLoanId();
        final LoanData loanData = loanId == 0 ? null : new LoanData(loanId, ratingSupplier);
        return new SourceAgnosticTransfer(TransferSource.BLOCKED_AMOUNT, blockedAmount.getDateStart(),
                                          TransactionOrientation.OUT, blockedAmount.getCategory(),
                                          blockedAmount.getAmount(), loanData);
    }

    public TransferSource getSource() {
        return source;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Optional<LoanData> getLoanData() {
        return Optional.ofNullable(loanData);
    }

    private int getLoanId() {
        return (loanData == null ? 0 : loanData.getId());
    }

    public OffsetDateTime getDateTime() {
        return dateTime;
    }

    public void promote(final TransferSource newSource) {
        if (source.canBePromotedTo(newSource)) {
            LOGGER.debug("Promoting to {}: {}.", newSource, this);
            source = newSource;
        } else {
            throw new IllegalArgumentException("Cannot promote " + source + " to " + newSource);
        }
    }

    @Override
    public String toString() {
        return "SourceAgnosticTransfer{" +
                "amount=" + amount +
                ", category=" + category +
                ", dateTime=" + dateTime +
                ", loanId=" + getLoanId() +
                ", source=" + source +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final SourceAgnosticTransfer that = (SourceAgnosticTransfer) o;
        return getLoanId() == that.getLoanId() &&
                category == that.category &&
                Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLoanId(), category, amount);
    }

    public static final class LoanData {

        private final int id;
        private final Supplier<Rating> rating;

        private LoanData(final int loanId, final Supplier<Rating> rating) {
            this.id = loanId;
            this.rating = rating;
        }

        public int getId() {
            return id;
        }

        public Rating getRating() {
            return rating.get();
        }
    }
}
