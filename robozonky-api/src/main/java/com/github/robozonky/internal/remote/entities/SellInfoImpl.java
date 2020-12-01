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

import java.util.Objects;
import java.util.StringJoiner;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.SellFee;
import com.github.robozonky.api.remote.entities.SellInfo;

public class SellInfoImpl implements SellInfo {

    private Money sellPrice;
    private SellFeeImpl fee;
    private Money boughtFor;
    private Money remainingPrincipal;
    private Ratio discount;

    public SellInfoImpl() {
        // For JSON-B.
    }

    public SellInfoImpl(Money sellPrice) {
        this.sellPrice = Objects.requireNonNull(sellPrice);
    }

    public SellInfoImpl(Money sellPrice, Money sellFee) {
        this.sellPrice = Objects.requireNonNull(sellPrice);
        this.boughtFor = sellPrice;
        this.remainingPrincipal = sellPrice;
        this.discount = Ratio.ZERO;
        this.fee = new SellFeeImpl(sellFee);
    }

    /**
     *
     * @param investment to copy data from
     * @return no fee, no discount, remaining principal = sell price = unpaid principal
     */
    public static SellInfoImpl getBasicSale(Investment investment) {
        var result = new SellInfoImpl(investment.getPrincipal()
            .getUnpaid());
        result.boughtFor = investment.getPrincipal()
            .getTotal();
        result.discount = Ratio.ZERO;
        result.fee = new SellFeeImpl(Money.ZERO);
        result.remainingPrincipal = investment.getPrincipal()
            .getUnpaid();
        return result;
    }

    @Override
    public SellFee getFee() {
        return fee;
    }

    public void setFee(final SellFeeImpl fee) {
        this.fee = fee;
    }

    @Override
    public Money getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(final Money sellPrice) {
        this.sellPrice = sellPrice;
    }

    @Override
    public Money getBoughtFor() {
        return boughtFor;
    }

    public void setBoughtFor(final Money boughtFor) {
        this.boughtFor = boughtFor;
    }

    @Override
    public Money getRemainingPrincipal() {
        return remainingPrincipal;
    }

    public void setRemainingPrincipal(final Money remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    @Override
    public Ratio getDiscount() {
        return discount;
    }

    public void setDiscount(final Ratio discount) {
        this.discount = discount;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SellInfoImpl.class.getSimpleName() + "[", "]")
            .add("boughtFor='" + boughtFor + "'")
            .add("sellPrice='" + sellPrice + "'")
            .add("discount='" + discount + "'")
            .add("fee=" + fee)
            .add("remainingPrincipal='" + remainingPrincipal + "'")
            .toString();
    }
}
