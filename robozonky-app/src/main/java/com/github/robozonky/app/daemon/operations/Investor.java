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

package com.github.robozonky.app.daemon.operations;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Function;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.confirmations.RequestId;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.common.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Investor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Investor.class);
    private static final InvestOperation DRY_RUN = recommendedLoan -> {
        Investor.LOGGER.debug("Dry run. Otherwise would attempt investing: {}.", recommendedLoan);
        return new ZonkyResponse(recommendedLoan.amount().intValue());
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

    public static Investor build(final Tenant auth, final ConfirmationProvider provider, final char... password) {
        final InvestOperation o = auth.getSessionInfo().isDryRun() ? DRY_RUN : recommendedLoan -> {
            Investor.LOGGER.debug("Executing investment: {}.", recommendedLoan);
            final Investment i = Investor.convertToInvestment(recommendedLoan);
            try {
                auth.run(zonky -> zonky.invest(i));
                Investor.LOGGER.debug("Investment succeeded.");
                return new ZonkyResponse(i.getOriginalPrincipal().intValue());
            } catch (final Exception ex) {
                throw new IllegalStateException("Failed investing " + recommendedLoan.amount() + " CZK into "
                                                        + i.getLoanId(), ex);
            }
        };
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

    public ZonkyResponse invest(final RecommendedLoan r, final boolean alreadySeenBefore) {
        final boolean confirmationRequired = r.isConfirmationRequired();
        if (alreadySeenBefore) {
            Investor.LOGGER.debug("Loan seen before.");
            final boolean protectedByCaptcha = r.descriptor().getLoanCaptchaProtectionEndDateTime()
                    .map(date -> OffsetDateTime.now().isBefore(date))
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
                return new ZonkyResponse(ZonkyResponseType.SEEN_BEFORE);
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

    private ZonkyResponse investLocallyFailingOnCaptcha(final RecommendedLoan r) {
        return investOperation.apply(r);
    }

    private ZonkyResponse investOrDelegateOnCaptcha(final RecommendedLoan r) {
        final Optional<OffsetDateTime> captchaEndDateTime =
                r.descriptor().getLoanCaptchaProtectionEndDateTime();
        final boolean isCaptchaProtected = captchaEndDateTime.isPresent() &&
                captchaEndDateTime.get().isAfter(OffsetDateTime.now());
        final boolean confirmationSupported = this.provider != null;
        if (!isCaptchaProtected) {
            return this.investLocallyFailingOnCaptcha(r);
        } else if (confirmationSupported) {
            return this.delegateOrReject(r);
        }
        Investor.LOGGER.warn("CAPTCHA protected, no support for delegation. Not investing: {}.", r);
        return new ZonkyResponse(ZonkyResponseType.REJECTED);
    }

    private ZonkyResponse delegateOrReject(final RecommendedLoan r) {
        Investor.LOGGER.debug("Asking to confirm investment: {}.", r);
        final boolean delegationSucceeded = this.provider.requestConfirmation(this.requestId,
                                                                              r.descriptor().item().getId(),
                                                                              r.amount().intValue());
        if (delegationSucceeded) {
            Investor.LOGGER.debug("Investment confirmed delegated, not investing: {}.", r);
            return new ZonkyResponse(ZonkyResponseType.DELEGATED);
        } else {
            Investor.LOGGER.debug("Investment not confirmed delegated, not investing: {}.", r);
            return new ZonkyResponse(ZonkyResponseType.REJECTED);
        }
    }

    @FunctionalInterface
    private interface InvestOperation extends Function<RecommendedLoan, ZonkyResponse> {

    }
}
