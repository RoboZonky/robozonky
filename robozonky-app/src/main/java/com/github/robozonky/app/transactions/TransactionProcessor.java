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

package com.github.robozonky.app.transactions;

import java.util.function.Consumer;

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.common.tenant.Tenant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class TransactionProcessor implements Consumer<Transaction> {

    protected final Logger logger = LogManager.getLogger(getClass());

    protected static Investment lookupOrFail(final int loanId, final Tenant auth) {
        return auth.call(zonky -> zonky.getInvestmentByLoanId(loanId))
                .orElseThrow(() -> new IllegalStateException("Investment not found for loan #" + loanId));
    }

    abstract boolean isApplicable(final Transaction transaction);

    abstract void processApplicable(final Transaction transaction);

    @Override
    public final void accept(final Transaction transaction) {
        if (!isApplicable(transaction)) {
            logger.trace("Skipping: {}.", transaction);
            return;
        }
        logger.debug("Processing: {}.", transaction);
        processApplicable(transaction);
    }
}
