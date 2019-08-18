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

import java.util.Optional;
import java.util.function.Predicate;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;

/**
 * Due to the inability for enums to extend a base shared class, this enum has its twin in
 * {@link InvestmentFailureType}. {@link Result} will assume the behavior of the two to be completely identical.
 */
public enum PurchaseFailureType implements Predicate<ClientErrorException> {

    INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE"),
    ALREADY_HAVE_INVESTMENT("ALREADY_HAVE_INVESTMENT"),
    UNKNOWN(NotFoundException.class);

    private final String reason;
    private final Class<? extends ClientErrorException> expectedException;

    PurchaseFailureType(final String reason, final Class<? extends ClientErrorException> expectedException) {
        this.reason = reason;
        this.expectedException = expectedException;
    }

    PurchaseFailureType(final Class<? extends ClientErrorException> expectedException) {
        this(null, expectedException);
    }

    PurchaseFailureType(final String reason) {
        this(reason, BadRequestException.class);
    }

    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    @Override
    public boolean test(final ClientErrorException ex) {
        return FailureTypeUtil.matches(expectedException, ex, reason);
    }
}
