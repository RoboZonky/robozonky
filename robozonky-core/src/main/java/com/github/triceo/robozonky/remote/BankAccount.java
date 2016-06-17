/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.remote;

import javax.xml.bind.annotation.XmlElement;

public class BankAccount implements BaseEntity {

    private int id, accountNo, accountBank;
    private String accountName;

    BankAccount() {
        // for JAXB
    }

    @XmlElement
    public int getAccountBank() {
        return accountBank;
    }

    @XmlElement
    public String getAccountName() {
        return accountName;
    }

    @XmlElement
    public int getAccountNo() {
        return accountNo;
    }

    @XmlElement
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BankAccount{");
        sb.append("id=").append(id);
        sb.append(", accountNo=").append(accountNo);
        sb.append(", accountBank=").append(accountBank);
        sb.append(", accountName='").append(accountName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
