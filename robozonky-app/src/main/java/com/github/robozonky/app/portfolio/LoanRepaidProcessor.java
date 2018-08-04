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

import com.github.robozonky.api.notifications.LoanRepaidEvent;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.app.configuration.daemon.TransactionalPortfolio;
import com.github.robozonky.app.util.LoanCache;

class LoanRepaidProcessor extends TransactionProcessor {

    public static final LoanRepaidProcessor INSTANCE = new LoanRepaidProcessor();

    private LoanRepaidProcessor() {
        // singleton
    }

    @Override
    boolean filter(final SourceAgnosticTransaction transaction) {
        return transaction.getSource() == TransactionSource.REAL &&
                transaction.getCategory() == TransactionCategory.PAYMENT;
    }

    @Override
    void process(final SourceAgnosticTransaction transaction, final TransactionalPortfolio portfolio) {
        final Loan l = LoanCache.INSTANCE.getLoan(transaction.getLoanId(), portfolio.getTenant());
        portfolio.getTenant().call(z -> z.getInvestment(l)).ifPresent(investment -> {
            final boolean paidInFull = investment.getPaymentStatus()
                    .map(s -> s == PaymentStatus.PAID)
                    .orElse(false);
            if (!paidInFull) {
                return;
            }
            portfolio.fire(new LoanRepaidEvent(investment, l, portfolio.getPortfolio().calculateOverview()));
        });
    }
}
