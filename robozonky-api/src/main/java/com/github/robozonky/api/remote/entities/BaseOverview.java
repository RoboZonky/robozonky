/*
 * Copyright 2017 The RoboZonky Project
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

import javax.xml.bind.annotation.XmlElement;

abstract class BaseOverview extends BaseEntity {

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
}
