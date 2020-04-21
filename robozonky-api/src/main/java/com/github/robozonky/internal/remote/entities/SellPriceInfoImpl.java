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

import java.util.StringJoiner;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.SellFee;
import com.github.robozonky.api.remote.entities.SellPriceInfo;

public class SellPriceInfoImpl implements SellPriceInfo {

    private Money sellPrice;

    private SellFee fee;

    private Money boughtFor;

    private Money remainingPrincipal;

    private Ratio discount;

    public SellPriceInfoImpl() {
        // For JSON-B.
    }

    @Override
    public SellFee getFee() {
        return fee;
    }

    @Override
    public Money getSellPrice() {
        return sellPrice;
    }

    @Override
    public Money getBoughtFor() {
        return boughtFor;
    }

    @Override
    public Money getRemainingPrincipal() {
        return remainingPrincipal;
    }

    @Override
    public Ratio getDiscount() {
        return discount;
    }

    public void setSellPrice(final Money sellPrice) {
        this.sellPrice = sellPrice;
    }

    public void setFee(final SellFee fee) {
        this.fee = fee;
    }

    public void setBoughtFor(final Money boughtFor) {
        this.boughtFor = boughtFor;
    }

    public void setRemainingPrincipal(final Money remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    public void setDiscount(final Ratio discount) {
        this.discount = discount;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SellPriceInfoImpl.class.getSimpleName() + "[", "]")
            .add("boughtFor='" + boughtFor + "'")
            .add("sellPrice='" + sellPrice + "'")
            .add("discount='" + discount + "'")
            .add("fee=" + fee)
            .add("remainingPrincipal='" + remainingPrincipal + "'")
            .toString();
    }
}
