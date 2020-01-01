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

import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.Money;
import io.vavr.Lazy;

public class SellPriceInfo extends BaseEntity {

    @XmlElement
    private SellFee fee;

    // Strings to be represented as money.
    @XmlElement
    private String boughtFor;
    private final Lazy<Money> moneyBoughtFor = Lazy.of(() -> Money.from(boughtFor));

    @XmlElement
    private String remainingPrincipal;
    private final Lazy<Money> moneyRemainingPrincipal = Lazy.of(() -> Money.from(remainingPrincipal));

    @XmlElement
    private String discount;
    private final Lazy<Money> moneyDiscount = Lazy.of(() -> Money.from(discount));

    SellPriceInfo() {
        // For JAXB.
    }

    public SellFee getFee() {
        return fee;
    }

    public Money getBoughtFor() {
        return moneyBoughtFor.get();
    }

    public Money getRemainingPrincipal() {
        return moneyRemainingPrincipal.get();
    }

    public Money getDiscount() {
        return moneyDiscount.get();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SellPriceInfo.class.getSimpleName() + "[", "]")
                .add("boughtFor='" + boughtFor + "'")
                .add("discount='" + discount + "'")
                .add("fee=" + fee)
                .add("remainingPrincipal='" + remainingPrincipal + "'")
                .toString();
    }
}
