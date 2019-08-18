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

package com.github.robozonky.internal.remote;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.ws.rs.ClientErrorException;

abstract class Result<T extends Predicate<ClientErrorException>> {

    private final T failureType;

    protected Result() {
        this.failureType = null;
    }

    protected Result(final ClientErrorException ex) {
        if (ex == null) {
            this.failureType = getForUnknown();
        } else {
            this.failureType = getAll()
                    .filter(f -> f.test(ex))
                    .findFirst()
                    .orElse(getForUnknown());
        }
    }

    protected abstract T getForUnknown();

    protected abstract Stream<T> getAll();

    public Optional<T> getFailureType() {
        return Optional.ofNullable(failureType);
    }

    public boolean isSuccess() {
        return failureType == null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final Result<T> result = (Result<T>) o;
        return Objects.equals(failureType, result.failureType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(failureType);
    }

    @Override
    public String toString() {
        return "Result{" +
                "failureType=" + failureType +
                '}';
    }
}
