/*
 * Copyright 2019 The RoboZonky Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robozonky.notifications.samples;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Development;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.*;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.entities.MutableDevelopment;
import com.github.robozonky.internal.remote.entities.MutableLoan;
import com.github.robozonky.internal.test.DateUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;
import static com.github.robozonky.internal.util.BigDecimalCalculator.plus;

final class Util {

    private static final Random RANDOM = new Random(0); // generate the same entities every time
    private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. In consectetur " +
            "dolor lorem, eu finibus dolor aliquet eleifend. Etiam lectus massa, dapibus vitae dictum non, pretium sed " +
            "libero. Etiam eu risus porttitor, scelerisque risus dictum, posuere orci. Nulla mattis in mauris nec " +
            "consectetur. Donec sodales libero commodo lorem lacinia dignissim. Integer pharetra est sit amet tortor " +
            "gravida egestas. Phasellus quis pellentesque dui, eget pretium elit. Ut non lobortis metus. Maecenas " +
            "sodales sit amet dolor eu ornare. Proin ac malesuada ligula, a laoreet nibh. Aliquam arcu velit, " +
            "posuere in bibendum viverra, ornare vitae nisl. Morbi ut dolor vitae nibh faucibus volutpat id eu ipsum." +
            " Cras luctus dolor ac ullamcorper consequat. Vivamus eget erat convallis elit volutpat consectetur id ut" +
            " augue. Duis dapibus suscipit massa id iaculis. Fusce eget tortor est.";

    private Util() {
        // no external instances
    }

    private static <T> T randomize(final T[] values) {
        final Object[] withoutUnknown = Stream.of(values)
                .filter(v -> !Objects.equals(v.toString(), "UNKNOWN"))
                .toArray(Object[]::new);
        return (T) withoutUnknown[RANDOM.nextInt(withoutUnknown.length)];
    }

    private static String generateText(final int wordCount) {
        final String[] words = LOREM_IPSUM.split(" ");
        return Stream.of(words)
                .limit(wordCount)
                .collect(Collectors.joining(" "));
    }

    private static String generateShortText() {
        final int wordCount = 2 + RANDOM.nextInt(10);
        return generateText(wordCount);
    }

    private static String generateMediumText() {
        final int wordCount = 5 + RANDOM.nextInt(20);
        return generateText(wordCount);
    }

    public static Loan randomizeLoan() {
        final MutableLoan loan = new MutableLoan();
        loan.setId(100_000 + RANDOM.nextInt(900_000)); // six-digit number
        loan.setCountryOfOrigin(randomize(Country.values()));
        loan.setCurrency(Defaults.CURRENCY);
        loan.setMainIncomeType(randomize(MainIncomeType.values()));
        loan.setPurpose(randomize(Purpose.values()));
        loan.setRegion(randomize(Region.values()));
        // set basic financial properties
        final Integer amount = (2 + RANDOM.nextInt(68) * 10_0000); // from 20 to 700_000
        loan.setAmount(amount.toString());
        final int term = 6 + RANDOM.nextInt(76); // from 6 to 84
        loan.setTermInMonths(term);
        final BigDecimal annuity = divide(amount, term);
        loan.setAnnuity(annuity.toPlainString());
        // set insurance properties
        final boolean isInsured = RANDOM.nextBoolean();
        if (isInsured) {
            loan.setInsuranceActive(true);
            loan.setAnnuityWithInsurance(plus(annuity, 50).toPlainString());
        } else {
            loan.setInsuranceActive(false);
            loan.setAnnuityWithInsurance(annuity.toPlainString());
        }
        loan.setInsuranceHistory(Collections.emptyList());
        loan.setAdditionallyInsured(false);
        loan.setInsuredInFuture(false);
        // set rating and infer other dependent properties
        loan.setRating(randomize(Rating.values()));
        loan.setInterestRate(loan.getRating().getInterestRate());
        loan.setRevenueRate(loan.getRating().getMaximalRevenueRate());
        // set various dates
        loan.setDatePublished(OffsetDateTime.now().minusDays(3));
        loan.setDeadline(loan.getDatePublished().plusDays(2));
        // set textual properties
        loan.setUrl("https://app.zonky.cz/#/marketplace/detail/" + loan.getId() + "/");
        loan.setStory(LOREM_IPSUM);
        loan.setName(generateShortText());
        return loan;
    }

    public static Investment randomizeInvestment(final Loan loan) {
        return new Investment(loan, Money.from(200 + (RANDOM.nextInt(24) * 200L))); // from 200 to 5000
    }

    public static PortfolioOverview randomizePortfolioOverview() {
        final ZonedDateTime now = DateUtil.zonedNow();
        final Map<Rating, Integer> invested = Stream.of(Rating.values())
                .collect(Collectors.toMap(r -> r, r -> RANDOM.nextInt(10_000_000)));
        return new PortfolioOverview() {
            @Override
            public Money getInvested() {
                return Money.from(invested.values().stream().reduce(0, Integer::sum));
            }

            @Override
            public Money getInvested(Rating r) {
                return Money.from(invested.get(r));
            }

            @Override
            public Ratio getAnnualProfitability() {
                final BigDecimal min = getMinimalAnnualProfitability().bigDecimalValue();
                final BigDecimal max = getOptimalAnnualProfitability().bigDecimalValue();
                return Ratio.fromRaw(divide(plus(min, max), 2));
            }

            @Override
            public ZonedDateTime getTimestamp() {
                return now;
            }
        };
    }

    public static ExtendedPortfolioOverview randomizeExtendedPortfolioOverview() {
        final PortfolioOverview portfolioOverview = randomizePortfolioOverview();
        return new ExtendedPortfolioOverview() {
            @Override
            public Money getAtRisk() {
                return Stream.of(Rating.values())
                        .map(this::getAtRisk)
                        .reduce(Money.ZERO, Money::add);
            }

            @Override
            public Money getAtRisk(Rating r) {
                int nextRandom = RANDOM.nextInt(100);
                if (nextRandom == 0) {
                    return Money.ZERO;
                } else {
                    return getInvested(r).divideBy(nextRandom);
                }
            }

            @Override
            public Money getSellable() {
                return Stream.of(Rating.values())
                        .map(this::getSellable)
                        .reduce(Money.ZERO, Money::add);
            }

            @Override
            public Money getSellable(Rating r) {
                return getAtRisk(r);
            }

            @Override
            public Money getSellableFeeless() {
                return Stream.of(Rating.values())
                        .map(this::getSellableFeeless)
                        .reduce(Money.ZERO, Money::add);
            }

            @Override
            public Money getSellableFeeless(Rating r) {
                int nextRandom = RANDOM.nextInt(100);
                if (nextRandom == 0) {
                    return Money.ZERO;
                } else {
                    return getSellable(r).divideBy(nextRandom);
                }
            }

            @Override
            public Money getInvested() {
                return portfolioOverview.getInvested();
            }

            @Override
            public Money getInvested(Rating r) {
                return portfolioOverview.getInvested(r);
            }

            @Override
            public Ratio getAnnualProfitability() {
                return portfolioOverview.getAnnualProfitability();
            }

            @Override
            public ZonedDateTime getTimestamp() {
                return portfolioOverview.getTimestamp();
            }
        };
    }

    public static Collection<Development> randomizeDevelopments(final Loan loan) {
        final int count = RANDOM.nextInt(5);
        final List<MutableDevelopment> developments = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final MutableDevelopment development = new MutableDevelopment();
            development.setLoanId(loan.getId());
            development.setBusinessCode(randomize(DevelopmentType.values()));
            if (RANDOM.nextBoolean()) { // some notes will be empty
                development.setPublicNote(generateMediumText());
            }
            if (i == 0) {
                development.setDateFrom(Instant.EPOCH.atZone(Defaults.ZONE_ID).toOffsetDateTime());
            } else {
                development.setDateFrom(developments.get(i - 1).getDateTo().orElseThrow());
            }
            if (i < count - 1) { // last note will not have any toDate
                development.setDateTo(development.getDateFrom().plusSeconds(RANDOM.nextInt(1_000_000)));
            }
            developments.add(development);
        }
        Collections.reverse(developments);
        return new ArrayList<>(developments);
    }

}
