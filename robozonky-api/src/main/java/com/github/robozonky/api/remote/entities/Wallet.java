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

import java.math.BigDecimal;
import java.util.Currency;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.enums.InvestmentType;

public class Wallet extends BaseEntity {

    private int id;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal blockedBalance;
    private BigDecimal creditSum;
    private BigDecimal debitSum;
    private int variableSymbol;
    private InvestmentType investmentType;
    private BankAccount account;
    private Currency currency;

    private Wallet() {
        // for JAXB
    }

    public Wallet(final BigDecimal balance) {
        this(0, 0, balance, balance);
    }

    public Wallet(final BigDecimal balance, final BigDecimal availableBalance) {
        this(0, 0, balance, availableBalance);
    }

    public Wallet(final int id, final int variableSymbol, final BigDecimal balance, final BigDecimal availableBalance) {
        this.id = id;
        this.availableBalance = availableBalance;
        this.balance = balance;
        this.variableSymbol = variableSymbol;
        this.blockedBalance = balance.subtract(availableBalance);
        this.creditSum = balance;
        this.debitSum = BigDecimal.ZERO;
        this.investmentType = InvestmentType.INVESTOR;
    }

    @XmlElement
    public Currency getCurrency() {
        return currency;
    }

    @XmlElement
    public int getId() {
        return id;
    }

    @XmlElement
    public BigDecimal getBalance() {
        return balance;
    }

    @XmlElement
    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    @XmlElement
    public BigDecimal getBlockedBalance() {
        return blockedBalance;
    }

    @XmlElement
    public BigDecimal getCreditSum() {
        return creditSum;
    }

    @XmlElement
    public BigDecimal getDebitSum() {
        return debitSum;
    }

    @XmlElement
    public int getVariableSymbol() {
        return variableSymbol;
    }

    @XmlElement
    public InvestmentType getInvestmentType() {
        return investmentType;
    }

    @XmlElement
    public BankAccount getAccount() {
        return account;
    }
}
