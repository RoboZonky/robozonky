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

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.Transactional;
import com.github.robozonky.common.state.InstanceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class TransferProcessor implements BiConsumer<Stream<SourceAgnosticTransfer>, Transactional> {

    private static final String STATE_KEY = "seen";
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final BinaryOperator<SourceAgnosticTransfer> deduplicator = (a, b) -> a;

    protected static Investment lookupOrFail(final Loan loan, final Tenant auth) {
        return auth.call(zonky -> zonky.getInvestment(loan))
                .orElseThrow(() -> new IllegalStateException("Investment not found for loan " + loan.getId()));
    }

    protected static int getLoanId(final SourceAgnosticTransfer transfer) {
        return transfer.getLoanData().map(SourceAgnosticTransfer.LoanData::getId).orElse(0);
    }

    abstract boolean filter(final SourceAgnosticTransfer transaction);

    abstract void process(final SourceAgnosticTransfer transaction, final Transactional portfolio);

    @Override
    public void accept(final Stream<SourceAgnosticTransfer> transfers, final Transactional transactional) {
        logger.debug("Processing {}.", this);
        final InstanceState<? extends TransferProcessor> state = transactional.getTenant().getState(this.getClass());
        final Set<Integer> alreadyProcessed = state
                .getValues(STATE_KEY)
                .orElse(Stream.empty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        logger.debug("Processed before: {}.", alreadyProcessed);
        final Set<Integer> newlyProcessed = new HashSet<>(0);
        transfers.filter(this::filter) // user-provided filter
                .filter(t -> t.getLoanData().isPresent()) // filter out all fees etc., we only care about loan-related
                .peek(t -> newlyProcessed.add(getLoanId(t))) // in the future, ignore present transfers
                .filter(t -> !alreadyProcessed.contains(getLoanId(t))) // ignore transfers we've processed before
                .peek(t -> logger.debug("Applicable: {}.", t))
                .collect(Collectors.toMap(TransferProcessor::getLoanId, t -> t, deduplicator)) // de-duplicate
                .values()
                .forEach(t -> process(t, transactional));
        state.update(m -> m.put(STATE_KEY, newlyProcessed.stream().map(String::valueOf)));
        logger.debug("Over.");
    }
}
