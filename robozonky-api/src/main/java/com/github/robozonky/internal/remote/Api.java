/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.internal.remote;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class Api<T> {

    private final T proxy;
    private final Timer timer;

    public Api(final T proxy) {
        this.proxy = proxy;
        this.timer = Timer.builder(UUID.randomUUID()
            .toString()) // Testing purposes
            .register(new SimpleMeterRegistry());
    }

    public Api(final T proxy, final Timer timer) {
        this.proxy = proxy;
        this.timer = Objects.requireNonNull(timer);
    }

    <S> S call(final Function<T, S> function) {
        return timer.record(() -> function.apply(proxy));
    }

    void run(final Consumer<T> consumer) {
        final Function<T, Boolean> wrapper = t -> {
            consumer.accept(t);
            return false; // we need to return something
        };
        call(wrapper);
    }
}
