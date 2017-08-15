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

package com.github.triceo.robozonky.app.investing;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.confirmations.RequestId;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.RecommendedLoan;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Investor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Investor.class);
    private final String username;
    private final Zonky zonky;
    private final boolean isDryRun;
    private final RequestId requestId;
    private final ConfirmationProvider provider;
    private Investor(final RequestId requestId, final ConfirmationProvider provider, final Zonky zonky,
                     final boolean isDryRun) {
        this.username = requestId.getUserId();
        this.zonky = zonky;
        this.isDryRun = isDryRun;
        this.provider = provider;
        this.requestId = requestId;
    }
    private Investor(final String username, final Zonky zonky, final boolean isDryRun) {
        this.username = username;
        this.zonky = zonky;
        this.isDryRun = isDryRun;
        this.provider = null;
        this.requestId = null;
    }

    static Investment convertToInvestment(final RecommendedLoan r) {
        final int amount = r.amount().intValue();
        return new Investment(r.descriptor().item(), amount);
    }

    public Zonky getZonky() {
        return zonky;
    }

    public boolean isDryRun() {
        return isDryRun;
    }

    public String getUsername() {
        return this.username;
    }

    public Optional<String> getConfirmationProviderId() {
        return (this.provider == null) ? Optional.empty() : Optional.of(this.provider.getId());
    }

    public ZonkyResponse invest(final RecommendedLoan r, final boolean alreadySeenBefore) {
        if (this.isDryRun) {
            Investor.LOGGER.debug("Dry run. Otherwise would attempt investing: {}.", r);
            return new ZonkyResponse(r.amount().intValue());
        } else if (alreadySeenBefore) {
            Investor.LOGGER.debug("Loan seen before.");
            final boolean protectedByCaptcha = r.descriptor().getLoanCaptchaProtectionEndDateTime()
                    .map(date -> OffsetDateTime.now().isBefore(date))
                    .orElse(false);
            final boolean confirmationRequired = r.isConfirmationRequired();
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
        } else {
            if (r.isConfirmationRequired()) {
                if (this.provider == null) {
                    throw new IllegalStateException("Confirmation required but no confirmation provider specified.");
                } else {
                    return this.delegateOrReject(r);
                }
            } else {
                return this.investOrDelegateOnCaptcha(r);
            }
        }
    }

    private ZonkyResponse investLocallyFailingOnCaptcha(final RecommendedLoan r) {
        Investor.LOGGER.debug("Executing investment: {}.", r);
        final Investment i = Investor.convertToInvestment(r);
        this.zonky.invest(i);
        Investor.LOGGER.debug("Investment succeeded.");
        return new ZonkyResponse(i.getAmount());
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

    public static class Builder {

        private String username = "";
        private boolean isDryRun = false;
        private ConfirmationProvider provider;
        private char[] password;

        public Investor.Builder usingConfirmation(final ConfirmationProvider provider, final char... password) {
            this.provider = provider;
            this.password = Arrays.copyOf(password, password.length);
            return this;
        }

        public Optional<ConfirmationProvider> getConfirmationUsed() {
            return Optional.ofNullable(provider);
        }

        public Optional<RequestId> getConfirmationRequestUsed() {
            return this.getConfirmationUsed().map(c -> new RequestId(username, password));
        }

        public Investor.Builder asUser(final String username) {
            this.username = username;
            return this;
        }

        public String getUsername() {
            return username;
        }

        public Investor.Builder asDryRun() {
            this.isDryRun = true;
            return this;
        }

        public boolean isDryRun() {
            return isDryRun;
        }

        public Investor build(final Zonky zonky) {
            return this.getConfirmationRequestUsed()
                    .map(r -> new Investor(r, provider, zonky, isDryRun))
                    .orElse(new Investor(username, zonky, isDryRun));
        }
    }
}
