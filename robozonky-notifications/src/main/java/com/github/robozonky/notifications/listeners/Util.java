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

package com.github.robozonky.notifications.listeners;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.LoanBased;
import com.github.robozonky.api.notifications.MarketplaceLoanBased;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.test.DateUtil;

import static com.github.robozonky.internal.util.BigDecimalCalculator.minus;
import static com.github.robozonky.internal.util.BigDecimalCalculator.plus;
import static java.util.Map.entry;

final class Util {

    private static final String AT = "@";
    private static final Pattern COMPILE = Pattern.compile("\\Q" + AT + "\\E");

    private Util() {
        // no instances
    }

    private static Date toDate(final LocalDate localDate) {
        return toDate(localDate.atStartOfDay(Defaults.ZONE_ID).toOffsetDateTime());
    }

    private static Date toDate(final OffsetDateTime offsetDateTime) {
        return Date.from(offsetDateTime.toInstant());
    }

    private static Date toDate(final ZonedDateTime zonedDateTime) {
        return Date.from(zonedDateTime.toInstant());
    }

    private static String identifyLoan(final MarketplaceLoan loan) {
        final BigDecimal interestRate = loan.getInterestRate().asPercentage();
        // string formatting ensures proper locale-specific floating point separator
        return String.format("č. %d (%.2f %% p.a., %d m.)", loan.getId(), interestRate, loan.getTermInMonths());
    }

    public static String identifyLoan(final MarketplaceLoanBased event) {
        return identifyLoan(event.getLoan());
    }

    public static String identifyLoan(final LoanBased event) {
        return identifyLoan(event.getLoan());
    }

    public static Map<String, Object> getLoanData(final MarketplaceLoan loan) {
        return Map.ofEntries(
                entry("loanId", loan.getId()),
                entry("loanAmount", loan.getAmount()),
                entry("loanAnnuity", loan.getAnnuity().intValue()),
                entry("loanInterestRate", loan.getRating().getCode()),
                entry("loanRating", loan.getRating()),
                entry("loanTerm", loan.getTermInMonths()),
                entry("loanUrl", loan.getUrl()),
                entry("loanRegion", loan.getRegion()),
                entry("loanMainIncomeType", loan.getMainIncomeType()),
                entry("loanPurpose", loan.getPurpose()),
                entry("loanName", loan.getName()),
                entry("insurance", loan.isInsuranceActive())
        );
    }

    private static Map<String, Object> perRating(final Function<Rating, Number> provider) {
        return Stream.of(Rating.values()).collect(Collectors.toMap(Rating::getCode, provider));
    }

    public static Map<String, Object> summarizePortfolioStructure(final PortfolioOverview portfolioOverview) {
        return Map.ofEntries(
                entry("absoluteShare", perRating(portfolioOverview::getCzkInvested)),
                entry("relativeShare", perRating(portfolioOverview::getShareOnInvestment)),
                entry("absoluteRisk", perRating(portfolioOverview::getCzkAtRisk)),
                entry("relativeRisk", perRating(portfolioOverview::getAtRiskShareOnInvestment)),
                entry("absoluteSellable", perRating(portfolioOverview::getCzkSellable)),
                entry("relativeSellable", perRating(portfolioOverview::getShareSellable)),
                entry("absoluteSellableFeeless", perRating(portfolioOverview::getCzkSellableFeeless)),
                entry("relativeSellableFeeless", perRating(portfolioOverview::getShareSellableFeeless)),
                entry("total", portfolioOverview.getCzkInvested()),
                entry("totalRisk", portfolioOverview.getCzkAtRisk()),
                entry("totalSellable", portfolioOverview.getCzkSellable()),
                entry("totalSellableFeeless", portfolioOverview.getCzkSellableFeeless()),
                entry("totalShare", portfolioOverview.getShareAtRisk()),
                entry("totalSellableShare", portfolioOverview.getShareSellable()),
                entry("totalSellableFeelessShare", portfolioOverview.getShareSellableFeeless()),
                entry("balance", portfolioOverview.getCzkAvailable()),
                entry("profitability", portfolioOverview.getAnnualProfitability()),
                entry("monthlyProfit", portfolioOverview.getCzkMonthlyProfit()),
                entry("minimalProfitability", portfolioOverview.getMinimalAnnualProfitability()),
                entry("minimalMonthlyProfit", portfolioOverview.getCzkMinimalMonthlyProfit()),
                entry("optimalProfitability", portfolioOverview.getOptimalAnnualProfitability()),
                entry("optimalMonthlyProfit", portfolioOverview.getCzkOptimalMonthyProfit()),
                entry("timestamp", toDate(portfolioOverview.getTimestamp()))
        );
    }

    public static boolean isNetworkProblem(final Throwable ex) {
        if (ex == null) {
            return false;
        } else if (ex instanceof SocketTimeoutException || ex instanceof SocketException) {
            return true;
        } else {
            return isNetworkProblem(ex.getCause());
        }
    }

    private static BigDecimal getTotalPaid(final Investment i) {
        return i.getPaidInterest()
                .add(i.getPaidPrincipal())
                .add(i.getPaidPenalty());
    }

    private static long getMonthsElapsed(final Investment i) {
        return Period.between(i.getInvestmentDate().toLocalDate(), DateUtil.localNow().toLocalDate()).toTotalMonths();
    }

    public static Map<String, Object> getLoanData(final Investment i, final MarketplaceLoan l) {
        final BigDecimal totalPaid = getTotalPaid(i);
        final BigDecimal originalPrincipal = i.getOriginalPrincipal();
        final BigDecimal balance = i.getSmpSoldFor()
                .map(soldFor -> {
                    final BigDecimal partial = minus(totalPaid, originalPrincipal);
                    final BigDecimal saleFee = i.getSmpFee().orElse(BigDecimal.ZERO);
                    return minus(plus(partial, soldFor), saleFee);
                })
                .orElseGet(() -> minus(totalPaid, originalPrincipal));
        final Map<String, Object> loanData = new HashMap<>(getLoanData(l));
        loanData.put("investedOn", Util.toDate(i.getInvestmentDate()));
        loanData.put("loanTermRemaining", i.getRemainingMonths());
        loanData.put("amountRemaining", i.getRemainingPrincipal());
        loanData.put("amountHeld", originalPrincipal);
        loanData.put("amountPaid", totalPaid);
        loanData.put("balance", balance);
        loanData.put("interestExpected", i.getExpectedInterest());
        loanData.put("interestPaid", i.getPaidInterest());
        loanData.put("penaltiesPaid", i.getPaidPenalty());
        loanData.put("monthsElapsed", getMonthsElapsed(i));
        loanData.put("insurance", i.isInsuranceActive()); // override the one coming from parent
        loanData.put("postponed", i.areInstalmentsPostponed());
        return loanData;
    }

    public static Map<String, Object> getDelinquentData(final Investment i, final Loan loan,
                                                        final Collection<Development> collections,
                                                        final LocalDate date) {
        final Map<String, Object> result = new HashMap<>(getLoanData(i, loan));
        result.put("since", Util.toDate(date));
        result.put("collections", collections.stream()
                .sorted(Comparator.comparing(Development::getDateFrom).reversed())
                .limit(5)
                .map(action -> {
                    final String code = action.getType().getCode();
                    final String note = action.getPublicNote().orElse("Bez dalšího vysvětlení.");
                    final Date dateFrom = Util.toDate(action.getDateFrom());
                    return action.getDateTo()
                            .map(dateTo -> Map.ofEntries(
                                    entry("code", code),
                                    entry("note", note),
                                    entry("startDate", dateFrom),
                                    entry("endDate", dateTo)))
                            .orElseGet(() -> Map.ofEntries(
                                    entry("code", code),
                                    entry("note", note),
                                    entry("startDate", dateFrom)));
                })
                .collect(Collectors.toList()));
        return result;
    }

    public static String stackTraceToString(final Throwable t) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    private static String cutMiddle(final String text) {
        final String separator = "...";
        if (text.length() < 2) {
            return text;
        } else {
            return text.charAt(0) + separator + text.charAt(text.length() - 1);
        }
    }

    public static String obfuscateEmailAddress(final String username) {
        if (username.contains(AT)) {
            final String[] parts = COMPILE.split(username.toLowerCase());
            return cutMiddle(parts[0].trim()) + AT + cutMiddle(parts[1].trim());
        } else {
            throw new IllegalArgumentException("Not a valid e-mail address: " + username);
        }
    }
}
