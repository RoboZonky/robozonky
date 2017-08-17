/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.triceo.robozonky.app.investing;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SessionState {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionState.class);
    private static final State.ClassSpecificState STATE = State.forClass(Session.class);
    private static final String SEEN_INVESTMENTS_ID = "seenInvestments",
            UNTOUCHABLE_INVESTMENTS_ID = "untouchableInvestments";

    private static Stream<LoanDescriptor> findLoanWithId(final int loanId,
                                                         final Collection<LoanDescriptor> knownLoans) {
        final Optional<LoanDescriptor> maybeLoan =
                knownLoans.stream().filter(loan -> loan.item().getId() == loanId).findFirst();
        return maybeLoan.map(Stream::of).orElse(Stream.empty());
    }

    private static Collection<LoanDescriptor> readInvestments(final Collection<LoanDescriptor> knownLoans,
                                                              final String propertyName) {
        final Optional<List<String>> result = SessionState.STATE.getValues(propertyName);
        return result.map(s -> s.stream()
                .map(Integer::parseInt)
                .distinct()
                .sorted()
                .flatMap(loanId -> SessionState.findLoanWithId(loanId, knownLoans))
                .collect(Collectors.toSet()))
                .orElse(new LinkedHashSet<>(0));
    }

    private static Collection<LoanDescriptor> readUntouchableInvestments(final Collection<LoanDescriptor> knownLoans) {
        return SessionState.readInvestments(knownLoans, SessionState.UNTOUCHABLE_INVESTMENTS_ID);
    }

    private static Collection<LoanDescriptor> readSeenInvestments(final Collection<LoanDescriptor> knownLoans) {
        return SessionState.readInvestments(knownLoans, SessionState.SEEN_INVESTMENTS_ID);
    }

    private static void writeInvestments(final String propertyName,
                                         final Collection<LoanDescriptor> rejectedInvestments) {
        final Stream<String> result = rejectedInvestments.stream()
                .map(l -> l.item().getId())
                .distinct()
                .sorted()
                .map(String::valueOf);
        SessionState.STATE.newBatch().set(propertyName, result).call();
    }

    private static void writeUntouchableInvestments(final Collection<LoanDescriptor> rejectedInvestments) {
        SessionState.writeInvestments(SessionState.UNTOUCHABLE_INVESTMENTS_ID, rejectedInvestments);
    }

    private static void writeSeenInvestments(final Collection<LoanDescriptor> seenInvestments) {
        SessionState.writeInvestments(SessionState.SEEN_INVESTMENTS_ID, seenInvestments);
    }

    private final Collection<LoanDescriptor> discardedLoans, seenLoans;

    public SessionState(final Collection<LoanDescriptor> marketplace) {
        discardedLoans = SessionState.readUntouchableInvestments(marketplace);
        SessionState.LOGGER.debug("Loans previously discarded: {}", discardedLoans);
        seenLoans = SessionState.readSeenInvestments(marketplace);
        SessionState.LOGGER.debug("Loans previously seen: {}", seenLoans);
    }

    public synchronized Collection<LoanDescriptor> getDiscardedLoans() {
        return Collections.unmodifiableCollection(discardedLoans);
    }

    public synchronized Collection<LoanDescriptor> getSeenLoans() {
        return Collections.unmodifiableCollection(seenLoans);
    }

    synchronized void discard(final LoanDescriptor loan) {
        this.discardedLoans.add(loan);
        SessionState.writeUntouchableInvestments(this.discardedLoans);
    }

    synchronized void skip(final LoanDescriptor loan) {
        this.seenLoans.add(loan);
        SessionState.writeSeenInvestments(this.seenLoans);
    }
}
