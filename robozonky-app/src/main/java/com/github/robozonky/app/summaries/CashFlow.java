/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.summaries;

import java.math.BigDecimal;

final class CashFlow {

    private final CashFlow.Type type;
    private final BigDecimal amount;

    private CashFlow(final CashFlow.Type type, final BigDecimal amount) {
        this.type = type;
        this.amount = amount;
    }

    public static CashFlow fee(final BigDecimal amount) {
        return new CashFlow(Type.FEE, amount);
    }

    public static CashFlow external(final BigDecimal amount) {
        return new CashFlow(Type.EXTERNAL, amount);
    }

    public static CashFlow investment(final BigDecimal amount) {
        return new CashFlow(Type.INVESTMENT, amount);
    }

    public CashFlow.Type getType() {
        return type;
    }

    /**
     *
     * @return If positive, it means money coming into the user's Zonky wallet. If negative, money coming out.
     */
    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "CashFlow{" +
                "amount=" + amount +
                ", type=" + type +
                '}';
    }

    enum Type {
        FEE,
        EXTERNAL,
        INVESTMENT
    }
}
