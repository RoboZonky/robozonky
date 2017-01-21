/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Function;

import com.github.triceo.robozonky.api.confirmations.Confirmation;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.confirmations.RequestId;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZonkyProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZonkyProxy.class);

    public static final class Builder {

        private boolean isDryRun = false;
        private ConfirmationProvider provider;
        private RequestId requestId;

        public Builder usingConfirmation(final ConfirmationProvider provider, final String username,
                                         final char... password) {
            this.provider = provider;
            this.requestId = new RequestId(username, password);
            return this;
        }

        public Builder asDryRun() {
            this.isDryRun = true;
            return this;
        }

        public ZonkyProxy build(final ZonkyApi zonky) {
            if (this.provider == null) {
                return new ZonkyProxy(zonky, isDryRun);
            } else {
                return new ZonkyProxy(zonky, provider, requestId, isDryRun);
            }
        }

    }

    static Investment convertToInvestment(final Recommendation r, final Confirmation confirmation) {
        final int amount = (confirmation == null || !confirmation.getAmount().isPresent()) ?
                r.getRecommendedInvestmentAmount() :
                confirmation.getAmount().getAsInt();
        return new Investment(r.getLoanDescriptor().getLoan(), amount);
    }

    private final ZonkyApi zonky;
    private final boolean isDryRun;
    private final RequestId requestId;
    private final ConfirmationProvider provider;

    private ZonkyProxy(final ZonkyApi zonky, final ConfirmationProvider provider, final RequestId requestId,
                       final boolean isDryRun) {
        this.zonky = zonky;
        this.isDryRun = isDryRun;
        this.provider = provider;
        this.requestId = requestId;
    }

    private ZonkyProxy(final ZonkyApi zonky, final boolean isDryRun) {
        this(zonky, null, null, isDryRun);
    }

    public <T> T execute(final Function<ZonkyApi, T> operation) {
        return operation.apply(this.zonky);
    }

    public Optional<String> getConfirmationProviderId() {
        return (this.provider == null) ? Optional.empty() : Optional.of(this.provider.getId());
    }

    public ZonkyResponse invest(final Recommendation recommendation, final boolean alreadySeenBefore) {
        if (this.isDryRun) {
            ZonkyProxy.LOGGER.debug("Dry run. Otherwise would attempt investing: {}.", recommendation);
            return new ZonkyResponse(recommendation.getRecommendedInvestmentAmount());
        } else if (alreadySeenBefore) {
            ZonkyProxy.LOGGER.debug("Loan seen before.");
            final boolean protectedByCaptcha = recommendation.getLoanDescriptor().getLoanCaptchaProtectionEndDateTime()
                    .map(date -> OffsetDateTime.now().isBefore(date))
                    .orElse(false);
            final boolean confirmationRequired = recommendation.isConfirmationRequired();
            if (!protectedByCaptcha && !confirmationRequired) {
                /*
                 * investment is no longer protected by CAPTCHA and no confirmation is required. therefore we invest.
                 */
                return this.investLocallyFailingOnCaptcha(recommendation);
            } else {
                /*
                 * protected by captcha or confirmation required. yet already seen from a previous investment session.
                 * this must mean that the previous response was DELEGATED and the user did not respond in the
                 * meantime. we therefore keep the investment as delegated.
                 */
                return new ZonkyResponse(ZonkyResponseType.DELEGATED);
            }
        } else {
            if (recommendation.isConfirmationRequired()) {
                if (this.provider == null) {
                    ZonkyProxy.LOGGER.error("Confirmation required but no confirmation provider registered.");
                    return new ZonkyResponse(ZonkyResponseType.FAILED);
                } else {
                    return this.approveOrDelegate(recommendation);
                }
            } else {
                return this.investOrDelegateOnCaptcha(recommendation);
            }
        }
    }

    private ZonkyResponse investLocallyFailingOnCaptcha(final Recommendation recommendation, final Confirmation confirmation) {
        ZonkyProxy.LOGGER.debug("Executing investment: {}, confirmation: {}.", recommendation, confirmation);
        final Investment i = ZonkyProxy.convertToInvestment(recommendation, confirmation);
        this.zonky.invest(i);
        ZonkyProxy.LOGGER.debug("Investment succeeded.");
        return new ZonkyResponse(i.getAmount());
    }

    private ZonkyResponse investLocallyFailingOnCaptcha(final Recommendation recommendation) {
        return this.investLocallyFailingOnCaptcha(recommendation, null);
    }

    ZonkyResponse investOrDelegateOnCaptcha(final Recommendation recommendation) {
        final Optional<OffsetDateTime> captchaEndDateTime =
                recommendation.getLoanDescriptor().getLoanCaptchaProtectionEndDateTime();
        final boolean isCaptchaProtected = captchaEndDateTime.isPresent() &&
                captchaEndDateTime.get().isAfter(OffsetDateTime.now());
        final boolean confirmationSupported = this.provider != null;
        if (!isCaptchaProtected) {
            return this.investLocallyFailingOnCaptcha(recommendation);
        } else if (confirmationSupported) {
            return this.delegateOrReject(recommendation);
        }
        ZonkyProxy.LOGGER.warn("CAPTCHA protected, no support for delegation. Not investing: {}.", recommendation);
        return new ZonkyResponse(ZonkyResponseType.REJECTED);
    }

    ZonkyResponse approveOrDelegate(final Recommendation recommendation) {
        ZonkyProxy.LOGGER.debug("Asking to confirm investment: {}.", recommendation);
        final Optional<Confirmation> confirmation = this.provider.requestConfirmation(this.requestId,
                recommendation.getLoanDescriptor().getLoan().getId(),
                recommendation.getRecommendedInvestmentAmount());
        if (!confirmation.isPresent()) {
            ZonkyProxy.LOGGER.warn("Did not receive confirmation response, not investing: {}.", recommendation);
            return new ZonkyResponse(ZonkyResponseType.FAILED);
        }
        final Confirmation result = confirmation.get();
        switch (result.getType()) {
            case REJECTED:
                ZonkyProxy.LOGGER.debug("Negative confirmation received, not investing: {}.", recommendation);
                return new ZonkyResponse(ZonkyResponseType.REJECTED);
            case DELEGATED:
                ZonkyProxy.LOGGER.debug("Investment confirmed delegated, not investing: {}.", recommendation);
                return new ZonkyResponse(ZonkyResponseType.DELEGATED);
            case APPROVED:
                return this.investLocallyFailingOnCaptcha(recommendation, confirmation.get());
            default:
                throw new IllegalStateException("No way how this could happen.");
        }
    }

    private ZonkyResponse delegateOrReject(final Recommendation recommendation) {
        ZonkyProxy.LOGGER.debug("Asking to confirm investment: {}.", recommendation);
        final Optional<Confirmation> confirmation = this.provider.requestConfirmation(this.requestId,
                recommendation.getLoanDescriptor().getLoan().getId(),
                recommendation.getRecommendedInvestmentAmount());
        if (!confirmation.isPresent()) {
            ZonkyProxy.LOGGER.warn("Did not receive confirmation response, not investing: {}.", recommendation);
            return new ZonkyResponse(ZonkyResponseType.FAILED);
        }
        final Confirmation result = confirmation.get();
        switch (result.getType()) {
            case DELEGATED:
                ZonkyProxy.LOGGER.debug("Investment confirmed delegated, not investing: {}.", recommendation);
                return new ZonkyResponse(ZonkyResponseType.DELEGATED);
            default:
                ZonkyProxy.LOGGER.warn("Investment not delegated, not investing: {}.", recommendation);
                return new ZonkyResponse(ZonkyResponseType.REJECTED);
        }
    }

}
