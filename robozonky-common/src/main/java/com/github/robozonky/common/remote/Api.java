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

package com.github.robozonky.common.remote;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Api<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Api.class);

    private final T proxy;

    public Api(final T proxy) {
        this.proxy = proxy;
    }

    static <Y, Z> Z call(final Function<Y, Z> function, final Y proxy) {
        LOGGER.trace("Executing...");
        final BlockingOperation<Z> operation = new BlockingOperation<>(() -> function.apply(proxy));
        try {
            ForkJoinPool.managedBlock(operation);
        } catch (final InterruptedException ex) {
            LOGGER.debug("Remote operation interrupted.", ex);
            Thread.currentThread().interrupt();
        } finally {
            LOGGER.trace("... done.");
        }
        return operation.getResult();
    }

    <S> S call(final Function<T, S> function) {
        return call(function, proxy);
    }

    void run(final Consumer<T> consumer) {
        final Function<T, Boolean> wrapper = t -> {
            consumer.accept(t);
            return false; // we need to return something
        };
        call(wrapper);
    }
}
