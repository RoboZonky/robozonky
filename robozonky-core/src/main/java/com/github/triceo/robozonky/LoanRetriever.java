/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a {@link ForkJoinPool.ManagedBlocker} in order to use massively parallel API loan retrieval inside a
 * parallel stream. Use {@link #getLoan(ZonkyProxy, int)} as the entry point.
 */
class LoanRetriever implements ForkJoinPool.ManagedBlocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoanRetriever.class);

    /**
     * Retrieve the given loan from the API inside a managed block. Execute this method inside a {@link ForkJoinPool},
     * preferably in a parallel stream.
     * @param api API in question.
     * @param loanId ID of the loan to retrieve.
     * @return Missing if something went wrong during retrieval.
     */
    public static Optional<Loan> getLoan(final ZonkyProxy api, final int loanId) {
        final LoanRetriever lr = new LoanRetriever(api, loanId);
        try {
            ForkJoinPool.managedBlock(lr);
        } catch (final InterruptedException ex) {
            LoanRetriever.LOGGER.warn("Failed retrieving loan #{}.", loanId, ex);
            return Optional.empty();
        }
        return Optional.of(lr.getValue());
    }

    private final int loanId;
    private final ZonkyProxy api;
    private volatile Loan loan = null;

    private LoanRetriever(final ZonkyProxy api, final int loanId) {
        this.loanId = loanId;
        this.api = api;
    }

    @Override
    public boolean block() throws InterruptedException {
        if (this.loan == null) {
            this.loan = api.execute(zonky -> zonky.getLoan(loanId));
        }
        return true;
    }

    @Override
    public boolean isReleasable() {
        return (this.loan != null);
    }

    public Loan getValue() {
        return this.loan;
    }
}
