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

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class TransactionProcessor implements BiConsumer<Stream<Transaction>, Transactional> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final BinaryOperator<Transaction> deduplicator = (a, b) -> a;

    protected static Investment lookupOrFail(final Loan loan, final Tenant auth) {
        return auth.call(zonky -> zonky.getInvestment(loan))
                .orElseThrow(() -> new IllegalStateException("Investment not found for loan " + loan.getId()));
    }

    abstract boolean isApplicable(final Transaction transaction);

    abstract void processApplicable(final Transaction transaction, final Transactional portfolio);

    @Override
    public final void accept(final Stream<Transaction> transactions, final Transactional transactional) {
        transactions.filter(this::isApplicable) // user-provided filter
                .filter(t -> t.getLoanId() > 0) // filter out all fees etc., we only care about loan-related
                .peek(t -> logger.debug("Applicable: {}.", t))
                .collect(Collectors.toMap(Transaction::getLoanId, t -> t, deduplicator)) // de-duplicate
                .values()
                .forEach(t -> {
                    logger.debug("Will process: {}", t);
                    processApplicable(t, transactional);
                });
    }
}
