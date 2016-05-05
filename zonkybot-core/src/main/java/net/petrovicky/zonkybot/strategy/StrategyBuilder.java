package net.petrovicky.zonkybot.strategy;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import net.petrovicky.zonkybot.api.remote.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyBuilder.class);

    private final Map<Rating, StrategyPerRating> individualStrategies = new EnumMap<>(Rating.class);

    public StrategyBuilder addIndividualStrategy(Rating r, final BigDecimal targetShare, final int minTerm,
                                                 final int maxTerm, final int minAmount, final int maxAmount) {
        if (individualStrategies.containsKey(r)) {
            throw new IllegalArgumentException("Already added strategy for rating " + r);
        }
        individualStrategies.put(r, new StrategyPerRating(r, targetShare, minTerm, maxTerm, minAmount, maxAmount));
        LOGGER.info("Adding strategy for rating '{}'.", r.getDescription());
        LOGGER.debug("Target share for rating '{}' among total investments is {}.", r.getDescription(), targetShare);
        LOGGER.debug("Range of acceptable investment terms for rating '{}' is <{}, {}> months.", r.getDescription(),
                minTerm == -1 ? 0 : minTerm, maxTerm == -1 ? "+inf" : maxTerm);
        LOGGER.debug("Range of acceptable investment amounts for rating '{}' is <{}, {}> CZK.", r.getDescription(),
                minAmount, maxAmount);
        return this;
    }

    public InvestmentStrategy build() {
        if (individualStrategies.size() != Rating.values().length) {
            throw new IllegalStateException("Strategy is incomplete.");
        }
        return new InvestmentStrategy(individualStrategies);
    }

}
