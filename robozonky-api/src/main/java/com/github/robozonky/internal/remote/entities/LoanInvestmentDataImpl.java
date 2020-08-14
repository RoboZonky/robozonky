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

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.StringJoiner;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.LoanInvestmentData;
import com.github.robozonky.api.remote.enums.InvestmentStatus;

/**
 * Used to represent a participation in the authenticated bit of the API. Even
 * though the confusing name, this participation is also used to represent
 * participations of other users for a given loan.
 *
 * Beware, though, since selling a participation on the secondary marketplace
 * will result in that participation being replaced in the list of participations
 * under any given loan with a new one. So, while most of the participations will
 * have a creation date close to loan publishing date, occasionally some will be
 * much newer, indicating SMP transaction.
 *
 * To the best of our knowledge, there is no other way to detect such transactions
 * made by other users.
 */
public class LoanInvestmentDataImpl implements LoanInvestmentData {

    private long id;
    private long loanId;
    private Money amount;
    private InvestmentStatus status;
    private OffsetDateTime timeCreated;

    public LoanInvestmentDataImpl() {
        // For JSON-B.
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public long getLoanId() {
        return loanId;
    }

    public void setLoanId(final long loanId) {
        this.loanId = loanId;
    }

    @Override
    public Money getAmount() {
        return Objects.requireNonNull(amount);
    }

    public void setAmount(final Money amount) {
        this.amount = amount;
    }

    @Override
    public InvestmentStatus getStatus() {
        return Objects.requireNonNull(status);
    }

    public void setStatus(final InvestmentStatus status) {
        this.status = status;
    }

    @Override
    public OffsetDateTime getTimeCreated() {
        return Objects.requireNonNull(timeCreated);
    }

    public void setTimeCreated(final OffsetDateTime timeCreated) {
        this.timeCreated = timeCreated;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LoanInvestmentDataImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("loanId=" + loanId)
            .add("amount=" + amount)
            .add("status=" + status)
            .add("timeCreated=" + timeCreated)
            .toString();
    }
}
