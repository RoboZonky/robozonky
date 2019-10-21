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

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Due to the inability for enums to extend a base shared class, this enum has its twin in
 * {@link PurchaseFailureType}. {@link Result} will assume the behavior of the two to be completely identical.
 */
public enum InvestmentFailureType implements Predicate<ClientErrorException> {

    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS"),
    INSUFFICIENT_BALANCE("insufficientBalance"),
    CANCELLED("cancelled"),
    WITHDRAWN("withdrawn"),
    RESERVED_INVESTMENT_ONLY("reservedInvestmentOnly"),
    OVER_INVESTMENT("overInvestment"),
    MULTIPLE_INVESTMENT("multipleInvestment"),
    ALREADY_COVERED("alreadyCovered"),
    UNKNOWN(NotFoundException.class);

    private final String reason;
    private final Class<? extends ClientErrorException> expectedException;

    InvestmentFailureType(final String reason, final Class<? extends ClientErrorException> expectedException) {
        this.reason = reason;
        this.expectedException = expectedException;
    }

    InvestmentFailureType(final Class<? extends ClientErrorException> expectedException) {
        this(null, expectedException);
    }

    InvestmentFailureType(final String reason) {
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
