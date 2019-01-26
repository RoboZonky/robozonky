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
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Function;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.confirmations.RequestId;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.util.DateUtil;
import io.vavr.control.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Investor {

    private static final Logger LOGGER = LogManager.getLogger(Investor.class);
    private static final InvestOperation DRY_RUN = recommendedLoan -> {
        LOGGER.debug("Dry run. Otherwise would attempt investing: {}.", recommendedLoan);
        return Either.right(recommendedLoan.amount());
    };
    private final InvestOperation investOperation;
    private final RequestId requestId;
    private final ConfirmationProvider provider;

    private Investor(final RequestId requestId, final ConfirmationProvider provider, final InvestOperation operation) {
        this.investOperation = operation;
        this.provider = provider;
        this.requestId = requestId;
    }

    private Investor(final InvestOperation operation) {
        this.investOperation = operation;
        this.provider = null;
        this.requestId = null;
    }

    static Investment convertToInvestment(final RecommendedLoan r) {
        final int amount = r.amount().intValue();
        return Investment.fresh(r.descriptor().item(), amount);
    }

    public static Investor build(final Tenant auth) {
        return build(auth, null);
    }

    private static Either<InvestmentFailure, BigDecimal> invest(final Tenant auth,
                                                                final RecommendedLoan recommendedLoan) {
        LOGGER.debug("Executing investment: {}.", recommendedLoan);
        final Investment i = convertToInvestment(recommendedLoan);
        try {
            auth.run(zonky -> zonky.invest(i));
            LOGGER.info("Invested {} CZK into loan #{}.", recommendedLoan.amount(), i.getLoanId());
            return Either.right(recommendedLoan.amount());
        } catch (final Exception ex) {
            LOGGER.debug("Failed investing {} CZK into loan #{}. Likely already full in the meantime.",
                         recommendedLoan.amount(), i.getLoanId(), ex);
            return Either.left(InvestmentFailure.FAILED);
        }
    }

    public static Investor build(final Tenant auth, final ConfirmationProvider provider, final char... password) {
        final InvestOperation o = auth.getSessionInfo().isDryRun() ?
                DRY_RUN :
                recommendedLoan -> invest(auth, recommendedLoan);
        if (provider == null) {
            return new Investor(o);
        } else {
            final RequestId r = new RequestId(auth.getSessionInfo().getUsername(), password);
            return new Investor(r, provider, o);
        }
    }

    public Optional<ConfirmationProvider> getConfirmationProvider() {
        return Optional.ofNullable(provider);
    }

    public Either<InvestmentFailure, BigDecimal> invest(final RecommendedLoan r, final boolean alreadySeenBefore) {
        final boolean confirmationRequired = r.isConfirmationRequired();
        if (alreadySeenBefore) {
            LOGGER.debug("Loan seen before.");
            final boolean protectedByCaptcha = r.descriptor().getLoanCaptchaProtectionEndDateTime()
                    .map(date -> DateUtil.offsetNow().isBefore(date))
                    .orElse(false);
            if (!protectedByCaptcha && !confirmationRequired) {
                /*
                 * investment is no longer protected by CAPTCHA and no confirmation is required. therefore we invest.
                 */
                return this.investLocallyFailingOnCaptcha(r);
            } else {
                /*
                 * protected by captcha or confirmation required. yet already seen from a previous investment session.
                 * this must mean that the previous response was DELEGATED and the user did not respond in the
                 * meantime. we therefore keep the investment as delegated.
                 */
                return Either.left(InvestmentFailure.SEEN_BEFORE);
            }
        } else if (confirmationRequired) {
            if (this.provider == null) {
                throw new IllegalStateException("Confirmation required but no confirmation provider specified.");
            } else {
                return this.delegateOrReject(r);
            }
        } else {
            return this.investOrDelegateOnCaptcha(r);
        }
    }

    private Either<InvestmentFailure, BigDecimal> investLocallyFailingOnCaptcha(final RecommendedLoan r) {
        return investOperation.apply(r);
    }

    private Either<InvestmentFailure, BigDecimal> investOrDelegateOnCaptcha(final RecommendedLoan r) {
        final Optional<OffsetDateTime> captchaEndDateTime =
                r.descriptor().getLoanCaptchaProtectionEndDateTime();
        final boolean isCaptchaProtected = captchaEndDateTime.isPresent() &&
                captchaEndDateTime.get().isAfter(DateUtil.offsetNow());
        final boolean confirmationSupported = this.provider != null;
        if (!isCaptchaProtected) {
            return this.investLocallyFailingOnCaptcha(r);
        } else if (confirmationSupported) {
            return this.delegateOrReject(r);
        }
        LOGGER.warn("CAPTCHA protected, no support for delegation. Not investing: {}.", r);
        return Either.left(InvestmentFailure.REJECTED);
    }

    private Either<InvestmentFailure, BigDecimal> delegateOrReject(final RecommendedLoan r) {
        LOGGER.debug("Asking to confirm investment: {}.", r);
        final boolean delegationSucceeded = this.provider.requestConfirmation(this.requestId,
                                                                              r.descriptor().item().getId(),
                                                                              r.amount().intValue());
        if (delegationSucceeded) {
            LOGGER.debug("Investment confirmed delegated, not investing: {}.", r);
            return Either.left(InvestmentFailure.DELEGATED);
        } else {
            LOGGER.debug("Investment not confirmed delegated, not investing: {}.", r);
            return Either.left(InvestmentFailure.REJECTED);
        }
    }

    @FunctionalInterface
    private interface InvestOperation extends Function<RecommendedLoan, Either<InvestmentFailure, BigDecimal>> {

    }
}
