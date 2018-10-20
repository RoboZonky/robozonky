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

import com.github.robozonky.util.BlockingOperation;

class Api<T> {

    private final T proxy;

    public Api(final T proxy) {
        this.proxy = proxy;
    }

    <S> S call(final Function<T, S> function) {
        final BlockingOperation<S> operation = new BlockingOperation<>(() -> function.apply(proxy));
        try {
            ForkJoinPool.managedBlock(operation);
            return operation.getResult();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    void run(final Consumer<T> consumer) {
        final Function<T, Boolean> wrapper = t -> {
            consumer.accept(t);
            return Boolean.FALSE; // we need to return something
        };
        call(wrapper);
    }
}
