package net.petrovicky.zonkybot.remote;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class Investment {

    private final Loan loan;
    private final int investedAmount;

    public Investment(Loan loan, int investedAmount) {
        this.loan = loan;
        this.investedAmount = investedAmount;
    }

    @XmlTransient
    public Loan getLoan() {
        return loan;
    }

    @XmlElement
    public int getLoanId() {
        return this.loan.getId();
    }

    @XmlElement(name = "amount")
    public int getInvestedAmount() {
        return investedAmount;
    }
}
