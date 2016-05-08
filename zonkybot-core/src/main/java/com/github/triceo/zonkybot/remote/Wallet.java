/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.zonkybot.remote;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class Wallet {

    private int id;
    private BigDecimal balance, availableBalance, blockedBalance;
    private int variableSymbol;
    @XmlTransient
    private Object account;

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Wallet{");
        sb.append("id=").append(id);
        sb.append(", balance=").append(balance);
        sb.append(", availableBalance=").append(availableBalance);
        sb.append(", blockedBalance=").append(blockedBalance);
        sb.append('}');
        return sb.toString();
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
    public int getVariableSymbol() {
        return variableSymbol;
    }

    @XmlTransient
    public Object getAccount() {
        return account;
    }
}
