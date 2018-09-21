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

import java.time.LocalDate;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.Transactional;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.state.InstanceState;

public final class IncomeProcessor implements PortfolioDependant {

    static final String STATE_KEY = "lastSeenTransactionId";
    private static final BinaryOperator<Transaction> DEDUPLICATOR = (a, b) -> a;

    private static int processAllTransactions(final Stream<Transaction> transactions) {
        return transactions.mapToInt(Transaction::getId)
                .max()
                .orElse(-1);
    }

    private static void processTransaction(final TransactionProcessor processor, final Transaction transaction) {
        processor.accept(transaction);
    }

    private static int processNewTransactions(final Transactional transactional, final Stream<Transaction> transactions,
                                              final int lastSeenTransactionId) {
        return transactions.parallel() // retrieve remote pages in parallel
                .filter(t -> t.getId() > lastSeenTransactionId)
                .collect(Collectors.toMap(Transaction::getLoanId, t -> t, DEDUPLICATOR)) // de-duplicate
                .values()
                .parallelStream() // possibly thousands of transactions, process them in parallel
                .peek(t -> processTransaction(new LoanRepaidProcessor(transactional), t))
                .peek(t -> processTransaction(new ParticipationSoldProcessor(transactional), t))
                .mapToInt(Transaction::getId)
                .max()
                .orElse(lastSeenTransactionId);
    }

    @Override
    public void accept(final Transactional transactional) {
        final InstanceState<IncomeProcessor> state =
                transactional.getTenant().getState(IncomeProcessor.class);
        final int lastSeenTransactionId = state.getValue(STATE_KEY)
                .map(Integer::valueOf)
                .orElse(-1);
        // transactions from overnight processing have timestamps from the midnight of previous day
        final LocalDate lastUpdate = state.getLastUpdated()
                .map(u -> u.minusDays(1).toLocalDate())
                .orElse(LocalDate.now().minusWeeks(1));
        final Select sinceLastUpdate = new Select().greaterThanOrEquals("transaction.transactionDate", lastUpdate);
        final Tenant tenant = transactional.getTenant();
        final Stream<Transaction> transactions = tenant.call(z -> z.getTransactions(sinceLastUpdate));
        final int newLastSeenTransactionId = lastSeenTransactionId >= 0 ?
                processNewTransactions(transactional, transactions, lastSeenTransactionId) :
                processAllTransactions(transactions);
        state.update(m -> m.put(STATE_KEY, String.valueOf(newLastSeenTransactionId)));
    }
}
