/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.internal.remote.entities;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbProperty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Amounts;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.InvestmentLoanData;
import com.github.robozonky.api.remote.entities.LoanHealthStats;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.enums.Label;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.SellStatus;

public class InvestmentImpl implements Investment {

    private static final Logger LOGGER = LogManager.getLogger(InvestmentImpl.class);
    private long id;
    private InvestmentLoanDataImpl loan;
    @JsonbProperty(nillable = true)
    private SellInfoImpl smpSellInfo;
    private AmountsImpl principal;
    private AmountsImpl interest;
    private SellStatus sellStatus;

    public InvestmentImpl() {
        // For JSON-B.
    }

    public InvestmentImpl(final InvestmentLoanDataImpl loan, final Money amount) {
        this.principal = new AmountsImpl(amount);
        this.loan = loan;
    }

    /**
     * Loan health is notoriously difficult to retrieve properly.
     * It can be retrieved with certainty from {@link InvestmentLoanData#getHealthStats()},
     * but that requires an extra remote call.
     * Therefore this method does as much as it could to detect loan health from other signs first
     * and only does the remote call as a last resort.
     *
     * @param investment
     * @return never null
     */
    public static LoanHealth determineHealth(Investment investment) {
        var loanData = investment.getLoan();
        if (loanData.getDpd() > 0) { // The investment is guaranteed due.
            LOGGER.debug("Investment {} determined {} based on DPD.", investment, LoanHealth.CURRENTLY_IN_DUE);
            return LoanHealth.CURRENTLY_IN_DUE;
        }
        var label = loanData.getLabel();
        if (label.isPresent()) {
            if (label.get() == Label.PAST_DUE_PREVIOUSLY) { // The investment is guaranteed previously due.
                LOGGER.debug("Investment {} determined {} based on label.", investment, LoanHealth.HISTORICALLY_IN_DUE);
                return LoanHealth.HISTORICALLY_IN_DUE;
            } else { // The investment is guaranteed healthy.
                LOGGER.debug("Investment {} determined {} based on label.", investment, LoanHealth.HEALTHY);
                return LoanHealth.HEALTHY;
            }
        } else { // Incur a remote call to figure out the actual health of the investment.
            var loanHealth = loanData.getHealthStats()
                .map(LoanHealthStats::getLoanHealthInfo)
                .orElse(LoanHealth.HEALTHY);
            LOGGER.debug("Investment {} confirmed {}.", investment, loanHealth);
            return loanHealth;
        }
    }

    /**
     * See {@link #determineHealth(Investment)} for why this is necessary.
     */
    public static Money determineSellPrice(Investment investment) {
        var unpaidPrincipal = investment.getPrincipal()
            .getUnpaid();
        if (InvestmentImpl.determineHealth(investment) == LoanHealth.HEALTHY) { // Guaranteed without discount.
            LOGGER.debug("Sell price for {} determined: {}.", investment, unpaidPrincipal);
            return unpaidPrincipal;
        }
        // Incur a remote call to figure out the actual sell price.
        var sellPrice = investment.getSmpSellInfo()
            .map(SellInfo::getSellPrice)
            .orElse(unpaidPrincipal);
        LOGGER.debug("Sell price for {} confirmed: {}.", investment, sellPrice);
        return sellPrice;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public InvestmentLoanData getLoan() {
        return requireNonNull(loan);
    }

    public void setLoan(final InvestmentLoanDataImpl loan) {
        this.loan = loan;
    }

    @Override
    public Optional<SellInfo> getSmpSellInfo() {
        return Optional.ofNullable(smpSellInfo);
    }

    public void setSmpSellInfo(final SellInfoImpl smpSellInfo) {
        this.smpSellInfo = smpSellInfo;
    }

    @Override
    public Amounts getPrincipal() {
        return requireNonNull(principal);
    }

    public void setPrincipal(final AmountsImpl principal) {
        this.principal = principal;
    }

    @Override
    public Amounts getInterest() {
        return requireNonNull(interest);
    }

    public void setInterest(final AmountsImpl interest) {
        this.interest = interest;
    }

    @Override
    public SellStatus getSellStatus() {
        return requireNonNull(sellStatus);
    }

    public void setSellStatus(final SellStatus sellStatus) {
        this.sellStatus = sellStatus;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InvestmentImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("loan=" + loan)
            .add("principal=" + principal)
            .add("interest=" + interest)
            .add("sellStatus=" + sellStatus)
            .add("smpSellInfo=" + smpSellInfo)
            .toString();
    }
}
