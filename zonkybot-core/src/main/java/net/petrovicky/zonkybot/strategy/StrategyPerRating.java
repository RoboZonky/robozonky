package net.petrovicky.zonkybot.strategy;

import java.math.BigDecimal;

import net.petrovicky.zonkybot.remote.Loan;
import net.petrovicky.zonkybot.remote.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyPerRating {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyPerRating.class);

    private final boolean preferLongerTerms;
    private final Rating rating;
    private final BigDecimal targetShare;
    private final int minimumAcceptableTerm, maximumAcceptableTerm, minimumInvestmentAmount, maximumInvestmentAmount;

    public StrategyPerRating(final Rating rating, final BigDecimal targetShare, final int minTerm,
                             final int maxTerm, final int minAmount, final int maxAmount,
                             final boolean preferLongerTerms) {
        this.rating = rating;
        this.minimumAcceptableTerm = minTerm < 0 ? 0 : minTerm;
        this.maximumAcceptableTerm = maxTerm < 0 ? Integer.MAX_VALUE : maxTerm;
        this.targetShare = targetShare;
        this.minimumInvestmentAmount = minAmount;
        this.maximumInvestmentAmount = maxAmount;
        this.preferLongerTerms = preferLongerTerms;
    }

    public boolean isPreferLongerTerms() {
        return preferLongerTerms;
    }

    public Rating getRating() {
        return rating;
    }

    public BigDecimal getTargetShare() {
        return targetShare;
    }

    public int getMinimumInvestmentAmount() {
        return minimumInvestmentAmount;
    }

    public int getMaximumInvestmentAmount() {
        return maximumInvestmentAmount;
    }

    public boolean isAcceptableTerm(Loan loan) {
        return loan.getTermInMonths() >= minimumAcceptableTerm && loan.getTermInMonths() <= maximumAcceptableTerm;
    }

    public boolean isAcceptableAmount(Loan loan) {
        return loan.getRemainingInvestment() >= minimumInvestmentAmount;
    }

    public boolean isAcceptable(Loan loan) {
        if (loan.getRating() != getRating()) {
            throw new IllegalStateException("Loan " + loan + " should never have gotten here.");
        } else if (!isAcceptableTerm(loan)) {
            LOGGER.debug("Loan '{}' rejected; strategy looking for loans with terms in range <{}, {}>.", loan,
                    minimumAcceptableTerm, maximumAcceptableTerm);
            return false;
        } else if (!isAcceptableAmount(loan)) {
            LOGGER.debug("Loan '{}' rejected; strategy looking for minimum investment of {} CZK.", loan,
                    minimumInvestmentAmount);
            return false;
        }
        return true;
    }
}
