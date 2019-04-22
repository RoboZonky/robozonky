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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class AbstractTransactionProcessor<T> implements Predicate<Transaction>,
                                                          Function<Transaction, T>,
                                                          Consumer<Transaction>,
                                                          Supplier<Stream<T>> {

    protected final Logger logger = LogManager.getLogger();
    private final Collection<T> values = new CopyOnWriteArraySet<>();

    @Override
    public Stream<T> get() {
        return values.stream();
    }

    @Override
    public void accept(final Transaction transaction) {
        if (!test(transaction)) {
            logger.trace("Skipping: {}.", transaction);
            return;
        }
        logger.debug("Processing: {}.", transaction);
        values.add(apply(transaction));
    }
}
