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

package com.github.robozonky.app.daemon;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.internal.remote.InvestmentFailureType;
import com.github.robozonky.internal.remote.InvestmentResult;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.util.functional.Either;

abstract class Investor {

    private static final Logger LOGGER = Audit.investing();

    private Investor() {
        // no external instances
    }

    public static Investor build(final Tenant auth) {
        if (auth.getSessionInfo()
            .isDryRun()) {
            return new Investor() {
                @Override
                public Either<InvestmentFailureType, Money> invest(final RecommendedLoan r) {
                    LOGGER.debug("Dry run. Otherwise would attempt investing: {}.", r);
                    return Either.right(r.amount());
                }
            };
        } else {
            return new Investor() {

                @Override
                public Either<InvestmentFailureType, Money> invest(final RecommendedLoan r) {
                    return Investor.invest(auth, r);
                }
            };
        }
    }

    private static Either<InvestmentFailureType, Money> invest(final Tenant auth,
            final RecommendedLoan recommendedLoan) {
        LOGGER.debug("Executing investment: {}.", recommendedLoan);
        var loan = recommendedLoan.descriptor()
            .item();
        var amount = recommendedLoan.amount()
            .getValue()
            .intValue();
        final InvestmentResult r = auth.call(zonky -> zonky.invest(loan, amount));
        if (r.isSuccess()) {
            return Either.right(recommendedLoan.amount());
        } else {
            return Either.left(r.getFailureType()
                .get()); // get() while !isSuccess() guaranteed by Result contract
        }
    }

    public abstract Either<InvestmentFailureType, Money> invest(final RecommendedLoan r);
}
