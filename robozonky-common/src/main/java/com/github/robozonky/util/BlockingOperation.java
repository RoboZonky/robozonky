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

package com.github.robozonky.util;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlockingOperation<T> implements ForkJoinPool.ManagedBlocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingOperation.class);

    private final Supplier<T> operation;
    private final AtomicReference<T> result = new AtomicReference<>();

    public BlockingOperation(final Supplier<T> operation) {
        this.operation = operation;
    }

    public T getResult() {
        return result.get();
    }

    @Override
    public boolean block() {
        LOGGER.trace("Running {} in {}.", operation, this);
        try {
            result.set(operation.get());
        } finally {
            LOGGER.trace("Finished.");
        }
        return isReleasable();
    }

    @Override
    public boolean isReleasable() {
        return getResult() != null;
    }
}
