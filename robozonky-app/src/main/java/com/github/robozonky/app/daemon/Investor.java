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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.internal.remote.InvestmentFailureType;
import com.github.robozonky.internal.remote.InvestmentResult;
import com.github.robozonky.internal.tenant.Tenant;
import io.vavr.control.Either;
import org.apache.logging.log4j.Logger;

abstract class Investor {

    private static final Logger LOGGER = Logging.investing();

    private Investor() {
        // no external instances
    }

    static Investment convertToInvestment(final RecommendedLoan r) {
        final int amount = r.amount().intValue();
        return Investment.fresh(r.descriptor().item(), amount);
    }

    public static Investor build(final Tenant auth) {
        if (auth.getSessionInfo().isDryRun()) {
            return new Investor() {
                @Override
                public Either<InvestmentFailureType, BigDecimal> invest(final RecommendedLoan r) {
                    LOGGER.debug("Dry run. Otherwise would attempt investing: {}.", r);
                    return Either.right(r.amount());
                }
            };
        } else {
            return new Investor() {

                @Override
                public Either<InvestmentFailureType, BigDecimal> invest(final RecommendedLoan r) {
                    return Investor.invest(auth, r);
                }
            };
        }
    }

    private static Either<InvestmentFailureType, BigDecimal> invest(final Tenant auth, final RecommendedLoan recommendedLoan) {
        LOGGER.debug("Executing investment: {}.", recommendedLoan);
        final Investment i = convertToInvestment(recommendedLoan);
        try {
            final InvestmentResult r = auth.call(zonky -> zonky.invest(i));
            if (r.isSuccess()) {
                LOGGER.info("Invested {} CZK into loan #{}.", recommendedLoan.amount(), i.getLoanId());
                return Either.right(recommendedLoan.amount());
            } else {
                return Either.left(r.getFailureType().get()); // get() while !isSuccess() guaranteed by Result contract
            }
        } catch (final Exception ex) {
            LOGGER.debug("Failed investing {} CZK into loan #{} for an unknown reason.",
                         recommendedLoan.amount(), i.getId(), ex);
            return Either.left(InvestmentFailureType.UNKNOWN);
        }
    }

    public abstract Either<InvestmentFailureType, BigDecimal> invest(final RecommendedLoan r);
}
