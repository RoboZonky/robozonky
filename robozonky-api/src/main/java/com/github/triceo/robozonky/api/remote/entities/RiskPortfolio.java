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

import javax.xml.bind.annotation.XmlElement;

import com.github.triceo.robozonky.api.remote.enums.Rating;

public class RiskPortfolio extends OverallPortfolio {

    private int totalAmount;
    private Rating rating;

    RiskPortfolio() {
        // for JAXB
    }

    public RiskPortfolio(final Rating rating, final int paid, final int unpaid, final int due) {
        super(paid, unpaid, due);
        this.rating = rating;
        this.totalAmount = paid + unpaid + due;
    }

    @XmlElement
    public int getTotalAmount() {
        return totalAmount;
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

}
