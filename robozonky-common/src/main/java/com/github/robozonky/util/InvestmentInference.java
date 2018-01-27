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

package com.github.robozonky.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvestmentInference {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvestmentInference.class);
    private final boolean fromSecondaryMarketplace;
    private final OffsetDateTime investmentDate;
    private final BigDecimal originalAmount, totalAmountPaid;

    private InvestmentInference(final Investment investment, final Loan loan) {
        final BigDecimal purchasePrice = investment.getPurchasePrice();
        this.fromSecondaryMarketplace = purchasePrice != null;
        this.investmentDate = getInvestmentDate(investment, loan);
        LOGGER.trace("Investment date determined to be {} for {}.", investmentDate, investment);
        this.originalAmount = getOriginalAmount(investment);
        LOGGER.trace("Original amount determined to be {} CZK for {}.", originalAmount, investment);
        this.totalAmountPaid = investment.getPaidInterest()
                .add(investment.getPaidPrincipal())
                .add(investment.getPaidPenalty());
    }

    public static InvestmentInference with(final Investment investment, final Loan loan) {
        return new InvestmentInference(investment, loan);
    }

    private static OffsetDateTime getInvestmentDate(final Investment i, final Loan l) {
        if (i.getInvestmentDate() != null) {
            return i.getInvestmentDate();
        } else if (l.getMyInvestment() != null) {
            return l.getMyInvestment().getTimeCreated();
        } else {
            final int monthsElapsed = i.getLoanTermInMonth() - i.getCurrentTerm() + 1;
            return i.getNextPaymentDate().minusMonths(monthsElapsed);
        }
    }

    public BigDecimal getTotalAmountPaid() {
        return totalAmountPaid;
    }

    private BigDecimal getOriginalAmount(final Investment i) {
        if (fromSecondaryMarketplace) {
            return i.getPurchasePrice();
        } else {
            return i.getAmount();
        }
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public Period getElapsed(final LocalDate now) {
        return Period.between(investmentDate.toLocalDate(), now);
    }
}
