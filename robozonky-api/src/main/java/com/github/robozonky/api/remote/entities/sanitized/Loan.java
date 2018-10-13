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

package com.github.robozonky.api.remote.entities.sanitized;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import com.github.robozonky.api.remote.entities.RawLoan;

public interface Loan extends MarketplaceLoan {

    static Loan sanitized(final RawLoan original) {
        return sanitize(original).build();
    }

    static LoanBuilder custom() {
        return new MutableLoanImpl();
    }

    static LoanBuilder sanitize(final RawLoan original) {
        return new MutableLoanImpl(original);
    }

    Optional<BigDecimal> getRemainingPrincipalToLoan();

    Optional<BigDecimal> getTotalPrincipalToLoan();

    Optional<BigDecimal> getRemainingPrincipalToBorrower();

    Optional<BigDecimal> getTotalPrincipalToBorrower();

    Collection<String> getKnownBorrowerNicknames();
}
