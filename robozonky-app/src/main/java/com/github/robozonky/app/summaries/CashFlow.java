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
