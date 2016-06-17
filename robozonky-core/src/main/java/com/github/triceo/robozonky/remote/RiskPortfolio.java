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
package com.github.triceo.robozonky.remote;

import javax.xml.bind.annotation.XmlElement;

public class RiskPortfolio extends OverallPortfolio {

    private int totalAmount;
    private Rating rating;

    RiskPortfolio() {
        // for JAXB
    }

    public RiskPortfolio(final Rating rating, final int paid, final int unpaid, final int due, final int totalAmount) {
        super(paid, unpaid, due);
        this.rating = rating;
        this.totalAmount = totalAmount;
    }

    @XmlElement
    public int getTotalAmount() {
        return totalAmount;
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RiskPortfolio{");
        sb.append("totalAmount=").append(totalAmount);
        sb.append(", rating=").append(rating);
        sb.append("} extends ");
        sb.append(super.toString());
        return sb.toString();
    }
}
