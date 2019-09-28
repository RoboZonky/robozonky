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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.LoanHealthInfo;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import io.vavr.Lazy;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Currency;

public class Participation extends BaseEntity {

    private OffsetDateTime deadline;
    private LocalDate nextPaymentDate;
    private int loanId;
    private int originalInstalmentCount;
    private int remainingInstalmentCount;
    private int userId;
    private long id;
    private long investmentId;
    private MainIncomeType incomeType;
    private Ratio interestRate;
    private LoanHealthInfo loanHealthInfo;
    private String loanName;
    private Purpose purpose;
    private Rating rating;
    private boolean willExceedLoanInvestmentLimit;
    private boolean insuranceActive;
    private boolean additionallyInsured;
    private Object loanInvestments;

    private Currency currency;
    @XmlElement
    private String remainingPrincipal;
    private final Lazy<Money> moneyRemainingPrincipal = Lazy.of(() -> Money.from(remainingPrincipal, currency));
    @XmlElement
    private String discount;
    private final Lazy<Money> moneyDiscount = Lazy.of(() -> Money.from(discount, currency));
    @XmlElement
    private String price;
    private final Lazy<Money> moneyPrice = Lazy.of(() -> Money.from(price, currency));

    @XmlElement
    public OffsetDateTime getDeadline() {
        return deadline;
    }

    @XmlElement
    public LocalDate getNextPaymentDate() {
        return nextPaymentDate;
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
    public Currency getCurrency() {
        return currency;
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
