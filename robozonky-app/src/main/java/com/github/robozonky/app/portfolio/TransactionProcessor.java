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

import com.github.robozonky.app.configuration.daemon.TransactionalPortfolio;
import com.github.robozonky.common.state.InstanceState;

abstract class TransactionProcessor implements BiConsumer<Stream<SourceAgnosticTransaction>, TransactionalPortfolio> {

    private static final String STATE_KEY = "seen";

    private final BinaryOperator<SourceAgnosticTransaction> deduplicator = (a, b) -> a;

    abstract boolean filter(final SourceAgnosticTransaction transaction);

    abstract void process(final SourceAgnosticTransaction transaction, final TransactionalPortfolio portfolio);

    @Override
    public void accept(final Stream<SourceAgnosticTransaction> transactions, final TransactionalPortfolio portfolio) {
        final InstanceState<? extends TransactionProcessor> state = portfolio.getTenant().getState(this.getClass());
        final Set<Integer> alreadyProcessed = state
                .getValues(STATE_KEY)
                .orElse(Stream.empty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        final Set<Integer> newlyProcessed = new HashSet<>(0);
        transactions.filter(this::filter) // user-provided filter
                .filter(t -> !alreadyProcessed.contains(t.getLoanId())) // ignore those we've already processed
                .collect(Collectors.toMap(SourceAgnosticTransaction::getLoanId, t -> t, deduplicator)) // de-duplicate
                .values()
                .forEach(t -> {
                    process(t, portfolio);
                    newlyProcessed.add(t.getLoanId());
                });
        state.update(m -> m.put(STATE_KEY, newlyProcessed.stream().map(String::valueOf)));
    }
}
