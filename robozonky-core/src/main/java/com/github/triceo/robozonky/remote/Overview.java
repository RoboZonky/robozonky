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

abstract class Overview implements BaseEntity {

    private int totalInvestment, principalPaid, interestPaid, investmentCount;

    @XmlElement
    public int getTotalInvestment() {
        return totalInvestment;
    }

    @XmlElement
    public int getPrincipalPaid() {
        return principalPaid;
    }

    @XmlElement
    public int getInterestPaid() {
        return interestPaid;
    }

    @XmlElement
    public int getInvestmentCount() {
        return investmentCount;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Overview{");
        sb.append("totalInvestment=").append(totalInvestment);
        sb.append(", principalPaid=").append(principalPaid);
        sb.append(", interestPaid=").append(interestPaid);
        sb.append(", investmentCount=").append(investmentCount);
        sb.append('}');
        return sb.toString();
    }
}
