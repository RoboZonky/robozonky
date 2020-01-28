/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.functional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Either<L, R> {

    private final Optional<L> left;
    private final Optional<R> right;

    private Either(final Optional<L> l, final Optional<R> r) {
        l.ifPresentOrElse(x -> { /* NOOP */ },
                          () -> r.ifPresentOrElse(y -> { /* NOOP */ },
                                                  () -> {
                                                      throw new IllegalStateException("Both left and right are empty.");
                                                  }
                          ));
        left = l;
        right = r;
    }

    public static <L, R> Either<L, R> left(final L value) {
        return new Either<>(Optional.of(value), Optional.empty());
    }

    public static <L, R> Either<L, R> right(final R value) {
        return new Either<>(Optional.empty(), Optional.of(value));
    }

    public <T> T map(final Function<? super L, ? extends T> lFunc, final Function<? super R, ? extends T> rFunc) {
        return left.<T>map(lFunc).orElseGet(() -> right.map(rFunc).get());
    }

    public <T> Either<T, R> mapLeft(final Function<? super L, ? extends T> lFunc) {
        return new Either<>(left.map(lFunc), right);
    }

    public <T> Either<L, T> mapRight(final Function<? super R, ? extends T> rFunc) {
        return new Either<>(left, right.map(rFunc));
    }

    public void apply(final Consumer<? super L> lFunc, final Consumer<? super R> rFunc) {
        left.ifPresent(lFunc);
        right.ifPresent(rFunc);
    }

    public R get() {
        return getOrElseThrow(left -> new NoSuchElementException());
    }

    public <X extends RuntimeException> R getOrElseThrow(Function<? super L, X> exceptionFunction) {
        return right.orElseThrow(() -> exceptionFunction.apply(left.orElseThrow()));
    }

    public R getOrElse(R value) {
        return right.orElse(value);
    }

    public R getOrElseGet(Function<L, R> value) {
        return right.orElseGet(() -> left.map(value).orElseThrow());
    }

    public <U> U fold(final Function<? super L, ? extends U> lFunc, final Function<? super R, ? extends U> rFunc) {
        return right.map(r -> (U) rFunc.apply(r)).orElse(left.map(lFunc).orElseThrow());
    }

    public boolean isRight() {
        return right.isPresent();
    }

    public boolean isLeft() {
        return !isRight();
    }

    public L getLeft() {
        return left.orElseThrow();
    }

    public R getRight() {
        return right.orElseThrow();
    }
}
