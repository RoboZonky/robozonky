package com.github.robozonky.app.summaries;

import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class CashFlowSummary {

    private static final Logger LOGGER = LogManager.getLogger();

    private final int inTotal;
    private final int inFromDeposits;
    private final int outTotal;
    private final int outFromFees;
    private final int outFromWithdrawals;

    private CashFlowSummary(final int totalIn, final int inFromDeposits, final int totalOut, final int outFromFees,
                            final int outFromWithdrawals) {
        this.inTotal = totalIn;
        this.inFromDeposits = inFromDeposits;
        this.outTotal = totalOut;
        this.outFromFees = outFromFees;
        this.outFromWithdrawals = outFromWithdrawals;
    }

    public static CashFlowSummary from(final Stream<CashFlow> cashFlowStream) {
        final DoubleAdder in = new DoubleAdder();
        final DoubleAdder inDeposits = new DoubleAdder();
        final DoubleAdder out = new DoubleAdder();
        final DoubleAdder outFees = new DoubleAdder();
        final DoubleAdder outWithdrawals = new DoubleAdder();
        cashFlowStream.forEach(cashFlow -> {
            final double amount = cashFlow.getAmount().doubleValue();
            final boolean isIn = amount > 0;
            if (isIn) {
                in.add(amount);
            } else {
                out.add(amount);
            }
            switch (cashFlow.getType()) {
                case FEE:
                    outFees.add(amount);
                    if (isIn) {
                        /*
                         * fee refunds can not be included in +totals, we're including them in -totals, as if the
                         * original fee never existed.
                         */
                        in.add(-amount);
                        out.add(amount);
                    }
                    return;
                case EXTERNAL:
                    final DoubleAdder target = isIn ? inDeposits : outWithdrawals;
                    target.add(amount);
                    return;
                default:
                    LOGGER.debug("Skipping cash flow type {}.", cashFlow.getType());
            }
        });
        return new CashFlowSummary(in.intValue(), inDeposits.intValue(), -out.intValue(), -outFees.intValue(),
                                   -outWithdrawals.intValue());
    }

    public int getInTotal() {
        return inTotal;
    }

    public int getInFromDeposits() {
        return inFromDeposits;
    }

    public int getOutTotal() {
        return outTotal;
    }

    public int getOutFromFees() {
        return outFromFees;
    }

    public int getOutFromWithdrawals() {
        return outFromWithdrawals;
    }

    @Override
    public String toString() {
        return "CashFlowSummary{" +
                "inTotal=" + inTotal +
                ", inFromDeposits=" + inFromDeposits +
                ", outTotal=" + outTotal +
                ", outFromFees=" + outFromFees +
                ", outFromWithdrawals=" + outFromWithdrawals +
                '}';
    }
}
