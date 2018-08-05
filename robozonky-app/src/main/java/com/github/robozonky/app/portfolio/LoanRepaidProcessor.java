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
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.app.configuration.daemon.Transactional;
import com.github.robozonky.app.util.LoanCache;

class LoanRepaidProcessor extends TransferProcessor {

    public static final LoanRepaidProcessor INSTANCE = new LoanRepaidProcessor();

    private LoanRepaidProcessor() {
        // singleton
    }

    @Override
    boolean filter(final SourceAgnosticTransfer transaction) {
        return transaction.getSource() == TransferSource.REAL &&
                transaction.getCategory() == TransactionCategory.PAYMENT;
    }

    @Override
    void process(final SourceAgnosticTransfer transfer, final Transactional transactional) {
        final Loan l = LoanCache.INSTANCE.getLoan(transfer.getLoanId(), transactional.getTenant());
        final Investment investment = lookupOrFail(l, transactional.getTenant());
        final boolean paidInFull = investment.getPaymentStatus()
                .map(s -> s == PaymentStatus.PAID)
                .orElse(false);
        if (!paidInFull) {
            logger.debug("Not yet repaid in full: {}.", transfer);
            return;
        }
        transactional.fire(new LoanRepaidEvent(investment, l, transactional.getPortfolio().calculateOverview()));
    }
}
