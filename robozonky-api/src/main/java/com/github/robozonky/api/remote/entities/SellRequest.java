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

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlElement;

public class SellRequest extends BaseEntity {

    private long investmentId;
    private BigDecimal feeAmount, remainingPrincipal;

    public SellRequest(final RawInvestment investment) {
        this.investmentId = investment.getId();
        this.remainingPrincipal = investment.getRemainingPrincipal();
        this.feeAmount = investment.getSmpFee();
    }

    @XmlElement
    public long getInvestmentId() {
        return investmentId;
    }

    @XmlElement
    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    @XmlElement
    public BigDecimal getRemainingPrincipal() {
        return remainingPrincipal;
    }
}
