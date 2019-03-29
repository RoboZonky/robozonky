package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SelingThrottleTest extends AbstractZonkyLeveragingTest {

    @Test
    void picksSmallestOneIfAllOverThreshold() {
        final Rating rating = Rating.A;
        final Investment i1 = Investment.custom()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.TEN)
                .build();
        final Investment i2 = Investment.custom()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.TEN.pow(2))
                .build();
        final Investment i3 = Investment.custom()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.ONE)
                .build();
        final PortfolioOverview portfolioOverview = mockPortfolioOverview(10_000);
        when(portfolioOverview.getCzkInvested(eq(rating))).thenReturn(BigDecimal.TEN);
        final Stream<RecommendedInvestment> recommendations = Stream.of(i1, i2, i3)
                .map(i -> new InvestmentDescriptor(i, () -> null))
                .map(d -> d.recommend(d.item().getRemainingPrincipal()))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
        final SellingThrottle t = new SellingThrottle();
        final Stream<RecommendedInvestment> throttled = t.apply(recommendations, portfolioOverview);
        assertThat(throttled)
                .extracting(r -> r.descriptor().item())
                .containsOnly(i3);
    }

    @Test
    void picksAllBelowThreshold() {
        final Rating rating = Rating.A;
        final Investment i1 = Investment.custom()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.TEN)
                .build();
        final Investment i2 = Investment.custom()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.TEN.pow(2))
                .build();
        final Investment i3 = Investment.custom()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.ONE)
                .build();
        final PortfolioOverview portfolioOverview = mockPortfolioOverview(10_000);
        when(portfolioOverview.getCzkInvested()).thenReturn(BigDecimal.valueOf(2200));
        final Stream<RecommendedInvestment> recommendations = Stream.of(i1, i2, i3)
                .map(i -> new InvestmentDescriptor(i, () -> null))
                .map(d -> d.recommend(d.item().getRemainingPrincipal()))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
        final SellingThrottle t = new SellingThrottle();
        final Stream<RecommendedInvestment> throttled = t.apply(recommendations, portfolioOverview);
        assertThat(throttled)
                .extracting(r -> r.descriptor().item())
                .containsOnly(i1, i3);
    }

    @Test
    void noInput() {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview(10_000);
        final Stream<RecommendedInvestment> recommendations = Stream.empty();
        final SellingThrottle t = new SellingThrottle();
        final Stream<RecommendedInvestment> throttled = t.apply(recommendations, portfolioOverview);
        assertThat(throttled).isEmpty();
    }
}
