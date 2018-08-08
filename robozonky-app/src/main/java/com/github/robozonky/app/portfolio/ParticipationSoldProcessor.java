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
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.Transactional;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.app.util.SoldParticipationCache;

class ParticipationSoldProcessor extends TransactionProcessor {

    public static final ParticipationSoldProcessor INSTANCE = new ParticipationSoldProcessor();

    private ParticipationSoldProcessor() {
        // singleton
    }

    @Override
    boolean isApplicable(final Transaction transfer) {
        return transfer.getOrientation() == TransactionOrientation.IN
                && transfer.getCategory() == TransactionCategory.SMP_SELL;
    }

    @Override
    void processApplicable(final Transaction transfer, final Transactional transactional) {
        final int loanId = transfer.getLoanId();
        final Tenant tenant = transactional.getTenant();
        final Loan l = LoanCache.INSTANCE.getLoan(loanId, tenant);
        final Investment i = lookupOrFail(l, tenant);
        transactional.fire(new InvestmentSoldEvent(i, l, transactional.getPortfolio().calculateOverview()));
        SoldParticipationCache.forTenant(tenant).markAsSold(loanId);
    }
}
