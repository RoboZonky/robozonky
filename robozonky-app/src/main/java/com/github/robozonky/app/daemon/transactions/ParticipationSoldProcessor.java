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

package com.github.robozonky.app.daemon.transactions;

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.app.daemon.LoanCache;
import com.github.robozonky.app.daemon.TransactionalPortfolio;
import com.github.robozonky.common.Tenant;

import static com.github.robozonky.app.events.EventFactory.investmentSold;
import static com.github.robozonky.app.events.EventFactory.investmentSoldLazy;

class ParticipationSoldProcessor extends TransactionProcessor {

    private final TransactionalPortfolio transactional;

    ParticipationSoldProcessor(final TransactionalPortfolio transactional) {
        this.transactional = transactional;
    }

    @Override
    boolean isApplicable(final Transaction transaction) {
        return transaction.getOrientation() == TransactionOrientation.IN
                && transaction.getCategory() == TransactionCategory.SMP_SELL;
    }

    @Override
    void processApplicable(final Transaction transaction) {
        final int loanId = transaction.getLoanId();
        final Tenant tenant = transactional.getTenant();
        SoldParticipationCache.forTenant(tenant).markAsSold(loanId);
        transactional.fire(investmentSoldLazy(() -> {
            final Investment i = lookupOrFail(loanId, tenant);
            final Loan l = LoanCache.get().getLoan(loanId, tenant);
            return investmentSold(i, l, transactional.getPortfolio().getOverview());
        }));
    }
}
