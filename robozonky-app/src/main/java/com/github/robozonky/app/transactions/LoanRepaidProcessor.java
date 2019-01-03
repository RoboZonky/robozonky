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

package com.github.robozonky.app.transactions;

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.app.tenant.PowerTenant;

import static com.github.robozonky.app.events.impl.EventFactory.loanRepaid;
import static com.github.robozonky.app.events.impl.EventFactory.loanRepaidLazy;

class LoanRepaidProcessor extends TransactionProcessor {

    private final PowerTenant tenant;

    LoanRepaidProcessor(final PowerTenant tenant) {
        this.tenant = tenant;
    }

    @Override
    boolean isApplicable(final Transaction transaction) {
        return transaction.getCategory() == TransactionCategory.PAYMENT
                && transaction.getOrientation() == TransactionOrientation.IN;
    }

    @Override
    void processApplicable(final Transaction transfer) {
        final int loanId = transfer.getLoanId();
        final Investment investment = lookupOrFail(loanId, tenant);
        final boolean paidInFull = investment.getPaymentStatus()
                .map(s -> s == PaymentStatus.PAID)
                .orElse(false);
        if (!paidInFull) {
            logger.debug("Not yet repaid in full: {}.", transfer);
            return;
        }
        tenant.fire(loanRepaidLazy(() -> {
            final Loan loan = tenant.getLoan(loanId);
            return loanRepaid(investment, loan, tenant.getPortfolio().getOverview());
        }));
    }
}
