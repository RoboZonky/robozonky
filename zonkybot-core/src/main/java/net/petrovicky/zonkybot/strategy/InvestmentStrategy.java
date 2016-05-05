package net.petrovicky.zonkybot.strategy;

import java.util.EnumMap;
import java.util.Map;

import net.petrovicky.zonkybot.api.remote.Loan;
import net.petrovicky.zonkybot.api.remote.Rating;

class InvestmentStrategy implements Strategy {

    @Override
    public boolean isAcceptable(Loan loan) {
        return individualStrategies.get(loan.getRating()).isAcceptable(loan);
    }

    private final Map<Rating, StrategyPerRating> individualStrategies = new EnumMap<>(Rating.class);

    InvestmentStrategy(Map<Rating, StrategyPerRating> individualStrategies) {
        this.individualStrategies.putAll(individualStrategies);
    }

}
