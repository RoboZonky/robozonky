/*
 * Copyright 2020 The RoboZonky Project
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
import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.Money;

public class SellRequest extends BaseEntity {

    private long investmentId;
    private BigDecimal remainingPrincipal;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal feeAmount;

    public SellRequest(final long investmentId, final SellInfo sellInfo) {
        this.investmentId = investmentId;
        this.remainingPrincipal = sellInfo.getPriceInfo()
                .getRemainingPrincipal()
                .getValue();
        this.feeAmount = sellInfo.getPriceInfo()
                .getFee()
                .getValue()
                .getValue();
        this.discount = sellInfo.getPriceInfo()
                .getDiscount()
                .getValue();
        this.price = sellInfo.getPriceInfo()
                .getSellPrice()
                .getValue();
    }

    public SellRequest(final Investment investment) {
        this.investmentId = investment.getId();
        this.remainingPrincipal = investment.getRemainingPrincipal()
                .orElseThrow()
                .getValue();
        this.feeAmount = investment.getSmpFee()
                .orElse(Money.ZERO)
                .getValue();
        this.discount = BigDecimal.ZERO;
        this.price = investment.getSmpPrice()
                .map(Money::getValue)
                .orElse(remainingPrincipal);
    }

    @XmlElement
    public long getInvestmentId() {
        return investmentId;
    }

    @XmlElement
    public BigDecimal getRemainingPrincipal() {
        return remainingPrincipal;
    }

    @XmlElement
    public BigDecimal getPrice() {
        return price;
    }

    @XmlElement
    public BigDecimal getDiscount() {
        return discount;
    }

    @XmlElement
    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SellRequest.class.getSimpleName() + "[", "]")
                .add("discount=" + discount)
                .add("feeAmount=" + feeAmount)
                .add("investmentId=" + investmentId)
                .add("price=" + price)
                .add("remainingPrincipal=" + remainingPrincipal)
                .toString();
    }
}
