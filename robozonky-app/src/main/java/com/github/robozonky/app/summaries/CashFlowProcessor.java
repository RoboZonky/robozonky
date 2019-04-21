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

package com.github.robozonky.app.summaries;

import java.math.BigDecimal;

import com.github.robozonky.api.remote.entities.Transaction;

final class CashFlowProcessor extends AbstractTransactionProcessor<CashFlow> {

    private static BigDecimal getIncomingAmount(final Transaction transaction) {
        return transaction.getAmount().abs();
    }

    private static BigDecimal getOutgoingAmount(final Transaction transaction) {
        return getIncomingAmount(transaction).negate();
    }

    @Override
    public CashFlow apply(final Transaction transaction) {
        switch (transaction.getCategory()) {
            case DEPOSIT:
                return CashFlow.external(getIncomingAmount(transaction));
            case WITHDRAW:
                return CashFlow.external(getOutgoingAmount(transaction));
            case INVESTMENT_FEE_RETURN:
                return CashFlow.fee(getIncomingAmount(transaction));
            case SMP_SALE_FEE:
            case INVESTMENT_FEE:
                return CashFlow.fee(getOutgoingAmount(transaction));
            case PAYMENT:
            case SMP_SELL:
                return CashFlow.investment(getIncomingAmount(transaction));
            case INVESTMENT:
            case SMP_BUY:
                return CashFlow.investment(getOutgoingAmount(transaction));
            default:
                throw new IllegalStateException("Unsupported transaction category: " + transaction.getCategory());
        }
    }

    @Override
    public boolean test(final Transaction transaction) {
        return (transaction.getAmount().signum() != 0); // skip empty transactions
    }
}
