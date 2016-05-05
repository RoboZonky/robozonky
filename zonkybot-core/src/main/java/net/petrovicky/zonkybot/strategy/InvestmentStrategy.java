package net.petrovicky.zonkybot.strategy;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import net.petrovicky.zonkybot.api.remote.Loan;
import net.petrovicky.zonkybot.api.remote.Rating;

public class InvestmentStrategy implements Strategy {

    @Override
    public boolean isAcceptable(Loan loan) {
        return individualStrategies.get(loan.getRating()).isAcceptable(loan);
    }

    private final Map<Rating, StrategyPerRating> individualStrategies = new EnumMap<>(Rating.class);
    private final Map<Rating, BigDecimal> targetShares = new EnumMap<>(Rating.class);
    private int minimumInvestmentAmount = Integer.MAX_VALUE;

    InvestmentStrategy(Map<Rating, StrategyPerRating> individualStrategies) {
        this.individualStrategies.putAll(individualStrategies);
        for (StrategyPerRating s : individualStrategies.values()) {
            minimumInvestmentAmount = Math.min(s.getMinimumInvestmentAmount(), minimumInvestmentAmount);
            targetShares.put(s.getRating(), s.getTargetShare());
        }
    }

    public BigDecimal getTargetShare(final Rating r) {
        return targetShares.get(r);
    }

    public int getMinimumInvestmentAmount() {
        return minimumInvestmentAmount;
    }

}
