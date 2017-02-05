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

package com.github.triceo.robozonky.api.remote.entities;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlElement;

public class Wallet extends BaseEntity {

    private int id;
    private BigDecimal balance, availableBalance, blockedBalance, creditSum, debitSum;
    private int variableSymbol;
    private BankAccount account;

    private Wallet() {
        // for JAXB
    }

    public Wallet(final int id, final int variableSymbol, final BigDecimal balance, final BigDecimal availableBalance) {
        this.id = id;
        this.availableBalance = availableBalance;
        this.balance = balance;
        this.variableSymbol = variableSymbol;
        this.blockedBalance = balance.subtract(availableBalance);
        this.creditSum = balance;
        this.debitSum = BigDecimal.ZERO;
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
    public BankAccount getAccount() {
        return account;
    }

}
