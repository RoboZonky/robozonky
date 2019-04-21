package com.github.robozonky.app.summaries;

import com.github.robozonky.api.notifications.LoanAndInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;

final class LoanAndInvestmentImpl implements LoanAndInvestment {

    private final Investment investment;
    private final Loan loan;

    public LoanAndInvestmentImpl(final Investment i, final Loan l) {
        this.investment = i;
        this.loan = l;
    }

    @Override
    public Investment getInvestment() {
        return investment;
    }

    @Override
    public Loan getLoan() {
        return loan;
    }
}
