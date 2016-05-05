package net.petrovicky.zonkybot.strategy;

import java.math.BigDecimal;

import net.petrovicky.zonkybot.api.remote.Loan;
import net.petrovicky.zonkybot.api.remote.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyPerRating implements Strategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyPerRating.class);

    private final Rating rating;
    private final BigDecimal targetShare;
    private final int minimumAcceptableTerm, maximumAcceptableTerm, minimumInvestmentAmount, maximumInvestmentAmount;

    public StrategyPerRating(final Rating rating, final BigDecimal targetShare, final int minTerm,
                             final int maxTerm, final int minAmount, final int maxAmount) {
        this.rating = rating;
        this.minimumAcceptableTerm = minTerm < 0 ? 0 : minTerm;
        this.maximumAcceptableTerm = maxTerm < 0 ? Integer.MAX_VALUE : maxTerm;
        this.targetShare = targetShare;
        this.minimumInvestmentAmount = minAmount;
        this.maximumInvestmentAmount = maxAmount;
    }

    public Rating getRating() {
        return rating;
    }

    public BigDecimal getTargetShare() {
        return targetShare;
    }

    public int getMinimumInvestmentAmount() {
        return this.minimumInvestmentAmount;
    }

    public int getMaximumInvestmentAmount() {
        return this.maximumInvestmentAmount;
    }

    public boolean isAcceptableTerm(Loan loan) {
        return loan.getTermInMonths() >= minimumAcceptableTerm && loan.getTermInMonths() <= maximumAcceptableTerm;
    }

    public boolean isAcceptableAmount(Loan loan) {
        return loan.getRemainingInvestment() >= minimumInvestmentAmount;
    }

    @Override
    public boolean isAcceptable(Loan loan) {
        if (loan.getRating() != this.getRating()) {
            throw new IllegalStateException("Loan " + loan + " should never have gotten here.");
        } else if (!this.isAcceptableTerm(loan)) {
            LOGGER.debug("Loan '{}' rejected; strategy looking for loans with terms in range <{}, {}>.", loan,
                    minimumAcceptableTerm, maximumAcceptableTerm);
            return false;
        } else if (!this.isAcceptableAmount(loan)) {
            LOGGER.debug("Loan '{}' rejected; strategy looking for minimum investment of {} CZK.", loan,
                    minimumInvestmentAmount);
            return false;
        }
        return true;
    }
}
