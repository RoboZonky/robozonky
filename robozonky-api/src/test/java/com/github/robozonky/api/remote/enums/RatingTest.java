package com.github.robozonky.api.remote.enums;

import java.time.Instant;

import com.github.robozonky.api.Ratio;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class RatingTest {

    @Test
    void someRatingsUnavailableBefore2019() {
        final Instant ratingsChange = Rating.MIDNIGHT_2019_03_18.minusSeconds(1);
        SoftAssertions.assertSoftly(softly -> {
            for (final Rating r : new Rating[]{Rating.AAE, Rating.AE}) {
                softly.assertThat(r.getMaximalRevenueRate(ratingsChange))
                        .as("Max revenue rate for " + r)
                        .isEqualTo(Ratio.ZERO);
                softly.assertThat(r.getMinimalRevenueRate(ratingsChange))
                        .as("Min revenue rate for " + r)
                        .isEqualTo(Ratio.ZERO);
                softly.assertThat(r.getFee(ratingsChange))
                        .as("Fee for " + r)
                        .isEqualTo(Ratio.ZERO);
            }
        });
    }

    @Test
    void feesBefore2018() {
        final Instant ratingsChange = Rating.MIDNIGHT_2017_09_01.minusSeconds(1);
        SoftAssertions.assertSoftly(softly -> {
            for (final Rating r : new Rating[]{Rating.AAAAA, Rating.AAAA, Rating.AAA, Rating.AA, Rating.A, Rating.B,
                    Rating.C, Rating.D}) {
                softly.assertThat(r.getFee(ratingsChange))
                        .as("Fee for " + r)
                        .isEqualTo(Ratio.fromPercentage(1));
            }
        });
    }

    @Test
    void allRatingsNowAvailable() {
        SoftAssertions.assertSoftly(softly -> {
            for (final Rating r : Rating.values()) {
                softly.assertThat(r.getMaximalRevenueRate())
                        .as("Max revenue rate for " + r)
                        .isGreaterThan(Ratio.ZERO);
                softly.assertThat(r.getMinimalRevenueRate())
                        .as("Min revenue rate for " + r)
                        .isGreaterThan(Ratio.ZERO);
                softly.assertThat(r.getFee())
                        .as("Fee for " + r)
                        .isGreaterThan(Ratio.ZERO);
                softly.assertThat(r.getInterestRate())
                        .as("Interest rate for " + r)
                        .isGreaterThan(Ratio.ZERO);
            }
        });
    }

    @Test
    void feesDecreasing() {
        SoftAssertions.assertSoftly(softly -> {
            for (final Rating r : Rating.values()) {
                final Ratio fee0 = r.getFee(0);
                final Ratio fee1 = r.getFee(150_000);
                final Ratio fee2 = r.getFee(200_000);
                final Ratio fee3 = r.getFee(500_000);
                final Ratio fee4 = r.getFee(1_000_000);
                softly.assertThat(fee1)
                        .as("Fee for " + r + " at 150 000")
                        .isLessThan(fee0);
                softly.assertThat(fee2)
                        .as("Fee for " + r + " at 200 000")
                        .isLessThan(fee1);
                softly.assertThat(fee3)
                        .as("Fee for " + r + " at 500 000")
                        .isLessThan(fee2);
                softly.assertThat(fee4)
                        .as("Fee for " + r + " at 1 000 000")
                        .isLessThan(fee3);
            }
        });
    }
}
