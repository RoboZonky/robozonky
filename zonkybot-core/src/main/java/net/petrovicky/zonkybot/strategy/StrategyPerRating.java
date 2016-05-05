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

    public boolean isAcceptableTerm(Loan loan) {
        return loan.getTermInMonths() >= minimumAcceptableTerm && loan.getTermInMonths() <= maximumAcceptableTerm;
    }

    public boolean isAcceptableAmount(Loan loan) {
        return loan.getAmount() > minimumInvestmentAmount;
    }

    @Override
    public boolean isAcceptable(Loan loan) {
        if (loan.getRating() != this.getRating()) {
            throw new IllegalStateException("Loan " + loan + " should never have gotten here.");
        } else if (!this.isAcceptableTerm(loan)) {
            LOGGER.info("Loan '{}' rejected; not within term limits defined in the strategy.", loan);
            return false;
        } else if (this.isAcceptableAmount(loan)) {
            LOGGER.info("Loan '{}' rejected; under the minimum investment amount defined in the strategy.", loan);
            return false;
        } else {
            // TODO implement maximum amount criteria
            // TODO implement share criteria
        }
        return true;
    }
}
