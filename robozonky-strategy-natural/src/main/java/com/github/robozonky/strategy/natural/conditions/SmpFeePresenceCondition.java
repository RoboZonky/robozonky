package com.github.robozonky.strategy.natural.conditions;

import java.math.BigDecimal;
import java.util.Optional;

public final class SmpFeePresenceCondition extends AbstractBooleanCondition {

    public static final MarketplaceFilterCondition PRESENT = new SmpFeePresenceCondition(true);
    public static final MarketplaceFilterCondition NOT_PRESENT = new SmpFeePresenceCondition(false);

    private SmpFeePresenceCondition(final boolean expectPresent) {
        super(w -> w.saleFee().orElse(BigDecimal.ZERO).signum() > 0, expectPresent);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Sale fee present: " + expected + ".");
    }
}
