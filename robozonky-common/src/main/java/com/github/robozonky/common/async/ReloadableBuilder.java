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

package com.github.robozonky.common.async;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.vavr.control.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Builds an instance of {@link Reloadable}.
 * @param <T> The type that the {@link Reloadable} should hold.
 */
public final class ReloadableBuilder<T> {

    private static final Consumer<?> NOOP_CONSUMER = x -> {
        // do nothing
    };
    private static final Logger LOGGER = LogManager.getLogger(ReloadableBuilder.class);

    private final Supplier<T> supplier;
    private UnaryOperator<T> reloader;
    private Function<T, Duration> reloadAfter;
    private Consumer<T> finisher;
    private boolean async = false;

    ReloadableBuilder(final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /**
     * When {@link Reloadable#get()} is called after this time passed since the remote operation was executed last,
     * the operation will be executed again. If the operation throws an exception, the reload will fail and there would
     * be no value. If not specified, the {@link Reloadable} will never reload, unless {@link Reloadable#clear()} is
     * called.
     * @param duration
     * @return This.
     */
    public ReloadableBuilder<T> reloadAfter(final Duration duration) {
        return reloadAfter(x -> duration);
    }

    /**
     * The same semantics as {@link #reloadAfter(Function)}, only the actual duration would be inferred from the value
     * of the {@link Reloadable} every time its new value is fetched.
     * @param durationSupplier
     * @return This.
     */
    public ReloadableBuilder<T> reloadAfter(final Function<T, Duration> durationSupplier) {
        this.reloadAfter = durationSupplier;
        return this;
    }

    /**
     * While the first instance will be loaded using the supplier provided in {@link #ReloadableBuilder(Supplier)},
     * every other instance will be retrieved using the function provided here.
     * @param reloader Previous instance retrieved by previous invocation of the supplier above or the function
     * provided here.
     * @return Fresh instance.
     */
    public ReloadableBuilder<T> reloadWith(final UnaryOperator<T> reloader) {
        this.reloader = reloader;
        return this;
    }

    /**
     * When the operation is successfully executed without errors, the given finisher has to be executed as well. If it
     * throws an exception, the operation itself is considered failed.
     * @param consumer
     * @return This.
     */
    public ReloadableBuilder<T> finishWith(final Consumer<T> consumer) {
        this.finisher = consumer;
        return this;
    }

    /**
     * If specified, {@link Reloadable#get()} will only trigger the operation on the background and return the
     * (now stale) value stored previously. If the background operation fails, the stale value will continue to be
     * returned. On the first {@link Reloadable#get()} call, the operation will be performed synchronously, as there
     * would otherwise be no stale value to return.
     * @return This.
     */
    public ReloadableBuilder<T> async() {
        this.async = true;
        return this;
    }

    /**
     * Build an initialized instance. Will call {@link #build()}, immediately following it up with a call to
     * {@link Reloadable#get()}.
     * @return New initialized instance.
     */
    public Either<Throwable, Reloadable<T>> buildEager() {
        final Reloadable<T> result = build();
        LOGGER.debug("Running before returning: {}.", result);
        final Either<Throwable, T> executed = result.get();
        return executed.map(r -> result);
    }

    /**
     * Build an empty instance. It will be initialized, executing the remote operation, whenever
     * {@link Reloadable#get()} is first called.
     * @return New instance.
     */
    public Reloadable<T> build() {
        final Consumer<T> finish = finisher == null ? (Consumer<T>) NOOP_CONSUMER : finisher;
        final UnaryOperator<T> reload = reloader == null ? t -> supplier.get() : reloader;
        if (reloadAfter == null) {
            return async ?
                    new AsyncReloadableImpl<>(supplier, reload, finish) :
                    new ReloadableImpl<>(supplier, reload, finish);
        } else {
            return async ?
                    new AsyncReloadableImpl<>(supplier, reload, finish, reloadAfter) :
                    new ReloadableImpl<>(supplier, reload, finish, reloadAfter);
        }
    }
}
