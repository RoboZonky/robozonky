package com.github.robozonky.api.notifications;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;

public interface LoanAndInvestment {

    Loan getLoan();

    Investment getInvestment();

}
