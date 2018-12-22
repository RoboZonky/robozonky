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

package com.github.robozonky.common.async;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReloadableBuilder<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadableBuilder.class);

    private final Supplier<T> supplier;
    private Duration reloadAfter;
    private Consumer<T> finisher;

    ReloadableBuilder(final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public ReloadableBuilder<T> reloadAfter(final Duration duration) {
        this.reloadAfter = duration;
        return this;
    }

    public ReloadableBuilder<T> finishWith(final Consumer<T> consumer) {
        this.finisher = consumer;
        return this;
    }

    public Either<Throwable, Reloadable<T>> buildEager() {
        final Reloadable<T> result = build();
        LOGGER.debug("Running before returning: {}.", result);
        final Either<Throwable, T> executed = result.get();
        return executed.map(r -> result);
    }

    public Reloadable<T> build() {
        if (finisher == null) {
            if (reloadAfter == null) {
                return new ReloadableImpl<>(supplier);
            } else {
                return new ReloadableImpl<>(supplier, reloadAfter);
            }
        } else {
            if (reloadAfter == null) {
                return new ReloadableImpl<>(supplier, finisher);
            } else {
                return new ReloadableImpl<>(supplier, reloadAfter, finisher);
            }
        }
    }
}
