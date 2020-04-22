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

public class InvestmentRequest {

    private int amount;
    private int loanId;

    public InvestmentRequest() {
        // For JSON-B.
    }

    public InvestmentRequest(int loanId, int amount) {
        this.amount = amount;
        this.loanId = loanId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(final int amount) {
        this.amount = amount;
    }

    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(final int loanId) {
        this.loanId = loanId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InvestmentRequest.class.getSimpleName() + "[", "]")
            .add("loanId=" + loanId)
            .add("amount=" + amount)
            .toString();
    }

}
