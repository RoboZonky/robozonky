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

package com.github.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

class Synthetic {

    private final int loanId;
    private final BigDecimal amount;

    public Synthetic(final int loanId, final BigDecimal amount, final OffsetDateTime timestamp) {
        this.loanId = loanId;
        this.amount = amount;
    }

    public int getLoanId() {
        return loanId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final Synthetic synthetic = (Synthetic) o;
        return loanId == synthetic.loanId &&
                Objects.equals(amount, synthetic.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId, amount);
    }

    @Override
    public String toString() {
        return "Synthetic{" +
                "amount=" + amount +
                ", loanId=" + loanId +
                '}';
    }
}
