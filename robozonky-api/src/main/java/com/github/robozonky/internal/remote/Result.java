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

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.ClientErrorException;

public final class Result {

    private static final Result SUCCESS = new Result();
    private final FailureType failureType;

    private Result() {
        this.failureType = null;
    }

    private Result(final FailureType type) {
        this.failureType = type;
    }

    public static Result success() {
        return SUCCESS;
    }

    public static Result failure(final ClientErrorException ex) {
        if (ex == null) {
            return new Result(FailureType.UNKNOWN);
        }
        final FailureType failure = Arrays.stream(FailureType.values())
                .filter(f -> f.test(ex))
                .findFirst()
                .orElse(FailureType.UNKNOWN);
        return new Result(failure);
    }

    public Optional<FailureType> getFailureType() {
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
        final Result result = (Result) o;
        return failureType == result.failureType;
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
