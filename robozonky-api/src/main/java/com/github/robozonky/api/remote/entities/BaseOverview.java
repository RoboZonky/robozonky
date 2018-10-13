/*
 * Copyright 2018 The RoboZonky Project
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

    private long totalInvestment, principalPaid, interestPaid, investmentCount, penaltyPaid;

    @XmlElement
    public long getTotalInvestment() {
        return totalInvestment;
    }

    @XmlElement
    public long getPrincipalPaid() {
        return principalPaid;
    }

    @XmlElement
    public long getInterestPaid() {
        return interestPaid;
    }

    @XmlElement
    public long getPenaltyPaid() {
        return penaltyPaid;
    }

    @XmlElement
    public long getInvestmentCount() {
        return investmentCount;
    }
}
