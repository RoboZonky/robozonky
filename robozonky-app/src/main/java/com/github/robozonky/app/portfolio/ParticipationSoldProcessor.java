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

import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.TransactionalPortfolio;
import com.github.robozonky.app.util.LoanCache;

class ParticipationSoldProcessor extends TransactionProcessor {

    public static final ParticipationSoldProcessor INSTANCE = new ParticipationSoldProcessor();

    private ParticipationSoldProcessor() {
        // singleton
    }

    private static Investment lookupOrFail(final Loan loan, final Tenant auth) {
        return auth.call(zonky -> zonky.getInvestment(loan))
                .orElseThrow(() -> new IllegalStateException("Investment not found for loan " + loan.getId()));
    }

    @Override
    boolean filter(final SourceAgnosticTransaction transaction) {
        return transaction.getCategory() == TransactionCategory.SMP_SELL;
    }

    @Override
    void process(final SourceAgnosticTransaction transaction, final TransactionalPortfolio portfolio) {
        final Tenant tenant = portfolio.getTenant();
        final Loan l = LoanCache.INSTANCE.getLoan(transaction.getLoanId(), tenant);
        final Investment i = lookupOrFail(l, tenant);
        portfolio.fire(new InvestmentSoldEvent(i, l, portfolio.getPortfolio().calculateOverview()));
    }
}
