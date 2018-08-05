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
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SourceAgnosticTransaction {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceAgnosticTransaction.class);

    private final int loanId;
    private final TransactionCategory category;
    private final BigDecimal amount;
    private final OffsetDateTime dateTime;
    private final Supplier<Rating> ratingSupplier;
    private TransactionSource source;

    private SourceAgnosticTransaction(final TransactionSource source, final OffsetDateTime dateTime,
                                      final int loanId,
                                      final TransactionOrientation orientation, final TransactionCategory category,
                                      final BigDecimal amount, final Supplier<Rating> ratingSupplier) {
        this(source, dateTime, loanId, category, normalizeAmount(orientation, amount), ratingSupplier);
    }

    private SourceAgnosticTransaction(final TransactionSource source, final OffsetDateTime dateTime,
                                      final int loanId, final TransactionCategory category, final BigDecimal amount,
                                      final Supplier<Rating> ratingSupplier) {
        this.source = source;
        this.dateTime = dateTime;
        this.loanId = loanId;
        this.category = category;
        this.amount = amount;
        this.ratingSupplier = ratingSupplier;
    }

    private static BigDecimal normalizeAmount(final TransactionOrientation orientation, final BigDecimal amount) {
        return (orientation == TransactionOrientation.IN) ? amount.abs() : amount.abs().negate();
    }

    public static SourceAgnosticTransaction synthetic(final OffsetDateTime dateTime, final int loanId,
                                                      final TransactionOrientation orientation,
                                                      final TransactionCategory category, final BigDecimal amount,
                                                      final Rating rating) {
        return new SourceAgnosticTransaction(TransactionSource.SYNTHETIC, dateTime, loanId, orientation,
                                             category, amount, () -> rating);
    }

    public static SourceAgnosticTransaction real(final Transaction transaction, final Supplier<Rating> ratingSupplier) {
        final OffsetDateTime date = transaction.getTransactionDate().atStartOfDay(Defaults.ZONE_ID).toOffsetDateTime();
        return new SourceAgnosticTransaction(TransactionSource.REAL, date, transaction.getLoanId(),
                                             transaction.getOrientation(),
                                             transaction.getCategory(), transaction.getAmount(), ratingSupplier);
    }

    public static SourceAgnosticTransaction blockation(final BlockedAmount blockedAmount,
                                                       final Supplier<Rating> ratingSupplier) {
        return new SourceAgnosticTransaction(TransactionSource.BLOCKED_AMOUNT,
                                             blockedAmount.getDateStart(),
                                             blockedAmount.getLoanId(), TransactionOrientation.OUT,
                                             blockedAmount.getCategory(), blockedAmount.getAmount(), ratingSupplier);
    }

    public TransactionSource getSource() {
        return source;
    }

    public int getLoanId() {
        return loanId;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Rating getRating() {
        return ratingSupplier.get();
    }

    public OffsetDateTime getDateTime() {
        return dateTime;
    }

    public void promote(final TransactionSource newSource) {
        if (source.canBePromotedTo(newSource)) {
            LOGGER.debug("Promoting to {}: {}.", newSource, this);
            source = newSource;
        } else {
            throw new IllegalArgumentException("Cannot promote " + source + " to " + newSource);
        }
    }

    @Override
    public String toString() {
        return "SourceAgnosticTransaction{" +
                "amount=" + amount +
                ", category=" + category +
                ", dateTime=" + dateTime +
                ", loanId=" + loanId +
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
        final SourceAgnosticTransaction that = (SourceAgnosticTransaction) o;
        return loanId == that.loanId &&
                category == that.category &&
                Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId, category, amount);
    }
}
