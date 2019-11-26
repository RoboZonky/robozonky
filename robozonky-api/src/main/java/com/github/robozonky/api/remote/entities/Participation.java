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

package com.github.robozonky.api.remote.entities;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Currency;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Country;
import com.github.robozonky.api.remote.enums.LoanHealthInfo;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.Defaults;
import io.vavr.Lazy;

public class Participation extends BaseEntity {

    protected long borrowerNo;
    protected int loanId;
    protected int originalInstalmentCount;
    protected int remainingInstalmentCount;
    protected int userId;
    protected long id;
    protected long investmentId;
    protected MainIncomeType incomeType;
    protected Ratio interestRate;
    protected LoanHealthInfo loanHealthInfo;
    protected String loanName;
    protected Purpose purpose;
    protected Rating rating;
    protected boolean willExceedLoanInvestmentLimit;
    protected boolean insuranceActive;
    protected boolean additionallyInsured;
    private Object loanInvestments;

    // dates and times are expensive to parse, and Participations are on the hot path. Only do it when needed.
    @XmlElement
    protected String deadline;
    private final Lazy<OffsetDateTime> deadlineParsed = Lazy.of(() -> OffsetDateTimeAdapter.fromString(deadline));
    @XmlElement
    protected String nextPaymentDate;
    private final Lazy<LocalDate> nextPaymentDateParsed = Lazy.of(() -> LocalDateAdapter.fromString(nextPaymentDate));

    protected Country countryOfOrigin = Defaults.COUNTRY_OF_ORIGIN;
    protected Currency currency;
    @XmlElement
    protected String remainingPrincipal;
    private final Lazy<Money> moneyRemainingPrincipal = Lazy.of(() -> Money.from(remainingPrincipal, currency));
    @XmlElement
    protected String discount;
    private final Lazy<Money> moneyDiscount = Lazy.of(() -> Money.from(discount, currency));
    @XmlElement
    protected String price;
    private final Lazy<Money> moneyPrice = Lazy.of(() -> Money.from(price, currency));

    Participation() {
        // for JAXB
        super();
    }

    public Participation(final Loan loan, final Money remainingPrincipal, final int remainingInstalmentCount) {
        super();
        this.loanId = loan.getId();
        this.borrowerNo = loan.getBorrowerNo();
        this.countryOfOrigin = loan.getCountryOfOrigin();
        this.currency = loan.getCurrency();
        this.incomeType = loan.getMainIncomeType();
        this.interestRate = loan.getInterestRate();
        this.loanName = loan.getName();
        this.originalInstalmentCount = loan.getTermInMonths();
        this.purpose = loan.getPurpose();
        this.rating = loan.getRating();
        this.userId = loan.getUserId();
        this.additionallyInsured = loan.isAdditionallyInsured();
        this.insuranceActive = loan.isInsuranceActive();
        this.remainingPrincipal = remainingPrincipal.getValue().toPlainString();
        this.remainingInstalmentCount = remainingInstalmentCount;
    }

    @XmlElement
    public long getId() {
        return id;
    }

    @XmlElement
    public long getInvestmentId() {
        return investmentId;
    }

    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    @XmlElement
    public long getBorrowerNo() {
        return borrowerNo;
    }

    @XmlElement
    public int getOriginalInstalmentCount() {
        return originalInstalmentCount;
    }

    @XmlElement
    public int getRemainingInstalmentCount() {
        return remainingInstalmentCount;
    }

    @XmlElement
    public int getUserId() {
        return userId;
    }

    @XmlElement
    public MainIncomeType getIncomeType() {
        return incomeType;
    }

    @XmlElement
    public Ratio getInterestRate() {
        return interestRate;
    }

    @XmlElement
    public String getLoanName() {
        return loanName;
    }

    @XmlElement
    public Purpose getPurpose() {
        return purpose;
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @XmlElement
    public boolean isWillExceedLoanInvestmentLimit() {
        return willExceedLoanInvestmentLimit;
    }

    /**
     * Semantics is identical to {@link BaseLoan#isInsuranceActive()} ()}.
     * @return
     */
    @XmlElement
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    /**
     * Semantics is identical to {@link BaseLoan#isAdditionallyInsured()}.
     * @return
     */
    @XmlElement
    public boolean isAdditionallyInsured() {
        return additionallyInsured;
    }

    @XmlElement
    public Object getLoanInvestments() { // FIXME figure out what this means
        return loanInvestments;
    }

    @XmlElement
    public LoanHealthInfo getLoanHealthInfo() {
        return loanHealthInfo;
    }

    @XmlElement
    public Country getCountryOfOrigin() {
        return countryOfOrigin;
    }

    @XmlElement
    public Currency getCurrency() {
        return currency;
    }

    @XmlTransient
    public OffsetDateTime getDeadline() {
        return deadlineParsed.get();
    }

    @XmlTransient
    public LocalDate getNextPaymentDate() {
        return nextPaymentDateParsed.get();
    }

    @XmlTransient
    public Money getRemainingPrincipal() {
        return moneyRemainingPrincipal.get();
    }

    @XmlTransient
    public Money getDiscount() {
        return moneyDiscount.get();
    }

    @XmlTransient
    public Money getPrice() {
        return moneyPrice.get();
    }

}
