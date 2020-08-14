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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.SellInfo;

public class SellRequest {

    private long investmentId;
    private BigDecimal remainingPrincipal;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal feeAmount;

    public SellRequest(final long investmentId, final SellInfo sellInfo) {
        this.investmentId = investmentId;
        this.remainingPrincipal = sellInfo.getRemainingPrincipal()
            .getValue();
        this.feeAmount = sellInfo.getFee()
            .getValue()
            .getValue();
        this.discount = sellInfo.getDiscount()
            .bigDecimalValue();
        this.price = sellInfo.getSellPrice()
            .getValue();
    }

    public SellRequest(final Investment investment) {
        this.investmentId = investment.getId();
        this.remainingPrincipal = investment.getPrincipal()
            .getUnpaid()
            .getValue();
        this.feeAmount = investment.getSmpSellInfo()
            .map(s -> s.getFee()
                .getValue())
            .orElse(Money.ZERO)
            .getValue();
        this.discount = BigDecimal.ZERO;
        this.price = investment.getSmpSellInfo()
            .map(s -> s.getSellPrice()
                .getValue())
            .orElse(remainingPrincipal);
    }

    public long getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(final long investmentId) {
        this.investmentId = investmentId;
    }

    public BigDecimal getRemainingPrincipal() {
        return remainingPrincipal;
    }

    public void setRemainingPrincipal(final BigDecimal remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(final BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(final BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
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
