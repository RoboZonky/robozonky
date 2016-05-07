/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.petrovicky.zonkybot.remote;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class Investment {

    private Loan loan;
    private int loanId, amount;

    public Investment(final Loan loan, final int amount) {
        this.loan = loan;
        this.loanId = loan.getId();
        this.amount = amount;
    }

    @XmlTransient
    public Loan getLoan() {
        return loan;
    }

    public void setLoan() {
        throw new UnsupportedOperationException("Cannot set loan other than from the constructor.");
    }

    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    @XmlElement
    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Investment{");
        sb.append("loanId=").append(loanId);
        sb.append(", amount=").append(amount);
        sb.append('}');
        return sb.toString();
    }
}
