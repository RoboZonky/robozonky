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

package com.github.robozonky.internal.api;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this to perform blocking operations inside {@link ForkJoinPool}s. All such operations will properly block,
 * spawning a new thread, instead of occupying the thread pool fully with operations that do nothing bug wait on I/O.
 * <p>
 * It will cache the first successful result of the operation, to be returned by all following calls.
 * @param <T> The return type of the operation.
 */
public class Retriever<T> implements ForkJoinPool.ManagedBlocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(Retriever.class);

    /**
     * Block until the retriever finishes the blocking operation, calling it repeatedly until it either succeeds or
     * throws {@link InterruptedException}.
     * @param retriever Retriever in question.
     * @param <T> Return type of the blocking operation.
     * @return Empty if interrupted, or if the operation result was empty.
     */
    static <T> Optional<T> retrieve(final Retriever<T> retriever) {
        try {
            ForkJoinPool.managedBlock(retriever);
            return retriever.getValue();
        } catch (final Exception ex) {
            Retriever.LOGGER.warn("Failed retrieving {}.", retriever);
            return Optional.empty();
        }
    }

    /**
     * Block until the the blocking operation is finished, calling it repeatedly until it either succeeds or
     * throws {@link InterruptedException}.
     * @param toExecute Blocking operation in question.
     * @param <T> Return type of the blocking operation.
     * @return Empty if interrupted, or if the operation result was empty.
     */
    public static <T> Optional<T> retrieve(final Supplier<Optional<T>> toExecute) {
        return Retriever.retrieve(new Retriever<>(toExecute));
    }

    private final Supplier<Optional<T>> toExecute;
    private final AtomicReference<Optional<T>> value = new AtomicReference<>();

    Retriever(final Supplier<Optional<T>> toExecute) {
        this.toExecute = toExecute;
    }

    @Override
    public boolean block() throws InterruptedException {
        if (!isReleasable()) {
            this.value.set(this.toExecute.get());
        }
        return isReleasable();
    }

    @Override
    public boolean isReleasable() {
        return (this.value.get() != null);
    }

    public Optional<T> getValue() {
        return this.value.get();
    }
}
