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

package com.github.robozonky.internal.remote.entities;

import java.math.BigDecimal;
import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.entities.SellRequest;

public class SellRequestImpl extends BaseEntity implements SellRequest {

    @XmlElement
    private long investmentId;
    @XmlElement
    private BigDecimal remainingPrincipal;
    @XmlElement
    private BigDecimal price;
    @XmlElement
    private BigDecimal discount;
    @XmlElement
    private BigDecimal feeAmount;

    public SellRequestImpl(final long investmentId, final SellInfo sellInfo) {
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
            .bigDecimalValue();
        this.price = sellInfo.getPriceInfo()
            .getSellPrice()
            .getValue();
    }

    public SellRequestImpl(final Investment investment) {
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

    @Override
    public long getInvestmentId() {
        return investmentId;
    }

    @Override
    public BigDecimal getRemainingPrincipal() {
        return remainingPrincipal;
    }

    @Override
    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public BigDecimal getDiscount() {
        return discount;
    }

    @Override
    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setInvestmentId(final long investmentId) {
        this.investmentId = investmentId;
    }

    public void setRemainingPrincipal(final BigDecimal remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    public void setDiscount(final BigDecimal discount) {
        this.discount = discount;
    }

    public void setFeeAmount(final BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SellRequestImpl.class.getSimpleName() + "[", "]")
            .add("discount=" + discount)
            .add("feeAmount=" + feeAmount)
            .add("investmentId=" + investmentId)
            .add("price=" + price)
            .add("remainingPrincipal=" + remainingPrincipal)
            .toString();
    }
}
