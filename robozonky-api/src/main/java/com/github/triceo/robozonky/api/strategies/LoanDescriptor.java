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

package com.github.triceo.robozonky.api.strategies;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.Optional;

import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.internal.api.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carries metadata regarding a {@link Loan}.
 */
public final class LoanDescriptor implements Descriptor<RecommendedLoan, LoanDescriptor, Loan> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoanDescriptor.class);

    private final Loan loan;

    public LoanDescriptor(final Loan loan) {
        this.loan = loan;
    }

    /**
     * If protected by CAPTCHA, gives the first instant when the CAPTCHA protection is over.
     * @return Present if loan protected by CAPTCHA, otherwise empty.
     */
    public Optional<OffsetDateTime> getLoanCaptchaProtectionEndDateTime() {
        final TemporalAmount captchaDelay = Settings.INSTANCE.getCaptchaDelay(loan.getRating());
        if (captchaDelay.get(ChronoUnit.SECONDS) == 0) {
            return Optional.empty();
        } else {
            return Optional.of(loan.getDatePublished().plus(captchaDelay));
        }
    }

    @Override
    public String toString() {
        return "LoanDescriptor{" +
                "loan=" + loan +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoanDescriptor)) {
            return false;
        }
        final LoanDescriptor that = (LoanDescriptor) o;
        return Objects.equals(loan, that.loan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loan);
    }

    @Override
    public Loan item() {
        return loan;
    }

    @Override
    public Loan related() {
        return loan;
    }

    /**
     * Convert the descriptor into an actual investment recommendation. This will be executed by the
     * {@link InvestmentStrategy}.
     * @param amount The amount recommended to invest.
     * @param confirmationRequired Whether or not {@link ConfirmationProvider} is required to confirm the investment.
     * @return Empty if amount is out of bounds.
     */
    public Optional<RecommendedLoan> recommend(final int amount, final boolean confirmationRequired) {
        if (amount >= Defaults.MINIMUM_INVESTMENT_IN_CZK && amount <= loan.getRemainingInvestment()) {
            return Optional.of(new RecommendedLoan(this, amount, confirmationRequired));
        } else {
            LOGGER.warn("Can not recommend {} CZK with {} CZK remaining in loan #{}.", amount,
                        loan.getRemainingInvestment(), loan.getId());
            return Optional.empty();
        }
    }

    public Optional<RecommendedLoan> recommend(final int toInvest) {
        return recommend(toInvest, false);
    }

    @Override
    public Optional<RecommendedLoan> recommend(final BigDecimal toInvest) {
        return recommend(toInvest.intValue());
    }
}
