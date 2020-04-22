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
import com.github.robozonky.api.remote.entities.RiskPortfolio;
import com.github.robozonky.api.remote.enums.Rating;

public class RiskPortfolioImpl implements RiskPortfolio {

    private Ratio interestRate;
    private Money unpaid;
    private Money paid;
    private Money due;
    private Money totalAmount;
    private Rating rating;

    public RiskPortfolioImpl() {
        // For JSON-B.
    }

    public RiskPortfolioImpl(final Rating rating, final Money paid, final Money unpaid, final Money due) {
        this.interestRate = rating.getInterestRate();
        this.paid = paid;
        this.unpaid = unpaid;
        this.due = due;
        this.rating = rating;
        this.totalAmount = paid.add(unpaid)
            .add(due);
    }

    @Override
    public Ratio getInterestRate() {
        return interestRate;
    }

    @Override
    public Money getUnpaid() {
        return unpaid;
    }

    @Override
    public Money getPaid() {
        return paid;
    }

    @Override
    public Money getDue() {
        return due;
    }

    @Override
    public Money getTotalAmount() {
        return totalAmount;
    }

    @Override
    public Rating getRating() {
        return rating;
    }

    public void setInterestRate(final Ratio interestRate) {
        this.interestRate = interestRate;
    }

    public void setUnpaid(final Money unpaid) {
        this.unpaid = unpaid;
    }

    public void setPaid(final Money paid) {
        this.paid = paid;
    }

    public void setDue(final Money due) {
        this.due = due;
    }

    public void setTotalAmount(final Money totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setRating(final Rating rating) {
        this.rating = rating;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RiskPortfolioImpl.class.getSimpleName() + "[", "]")
            .add("due='" + due + "'")
            .add("interestRate=" + interestRate)
            .add("paid='" + paid + "'")
            .add("rating=" + rating)
            .add("totalAmount='" + totalAmount + "'")
            .add("unpaid='" + unpaid + "'")
            .toString();
    }
}
