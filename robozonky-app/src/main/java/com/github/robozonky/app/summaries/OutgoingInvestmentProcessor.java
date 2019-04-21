/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.summaries;

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.app.tenant.PowerTenant;

final class OutgoingInvestmentProcessor extends AbstractTransactionProcessor<Investment> {

    private final PowerTenant tenant;

    OutgoingInvestmentProcessor(final PowerTenant tenant) {
        this.tenant = tenant;
    }

    @Override
    public Investment apply(final Transaction transaction) {
        return lookupOrFail(transaction.getLoanId(), tenant);
    }

    @Override
    public boolean test(final Transaction transaction) {
        if (transaction.getOrientation() != TransactionOrientation.IN) {
            return false;
        }
        switch (transaction.getCategory()) {
            case PAYMENT:
                final int loanId = transaction.getLoanId();
                final Investment investment = lookupOrFail(loanId, tenant);
                final boolean paidInFull = investment.getPaymentStatus()
                        .map(s -> s == PaymentStatus.PAID)
                        .orElse(false);
                if (!paidInFull) {
                    logger.debug("Not yet repaid in full: {}.", transaction);
                    return false;
                }
                return true;
            case SMP_SELL:
                return true;
            default:
                return false;
        }
    }
}
