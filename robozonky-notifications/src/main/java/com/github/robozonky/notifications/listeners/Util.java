/*
 * Copyright 2020 The RoboZonky Project
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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.LoanBased;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;

import static java.util.Map.entry;

final class Util {

    private static final String AT = "@";
    private static final Pattern COMPILE = Pattern.compile("\\Q" + AT + "\\E");

    private Util() {
        // no instances
    }

    public static Date toDate(final LocalDate localDate) {
        return toDate(localDate.atStartOfDay(Defaults.ZONE_ID).toOffsetDateTime());
    }

    public static Date toDate(final OffsetDateTime offsetDateTime) {
        return Date.from(offsetDateTime.toInstant());
    }

    public static Date toDate(final ZonedDateTime zonedDateTime) {
        return Date.from(zonedDateTime.toInstant());
    }

    private static String identifyLoan(final Loan loan) {
        final BigDecimal interestRate = loan.getInterestRate().asPercentage();
        // string formatting ensures proper locale-specific floating point separator
        return String.format("č. %d (%.2f %% p.a., %d m.)", loan.getId(), interestRate, loan.getTermInMonths());
    }

    public static String identifyLoan(final LoanBased event) {
        return identifyLoan(event.getLoan());
    }

    public static Map<String, Object> getLoanData(final Loan loan) {
        return Map.ofEntries(
                entry("loanId", loan.getId()),
                entry("loanAmount", loan.getAmount().getValue()),
                entry("loanAnnuity", loan.getAnnuity().getValue()),
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

    public static Map<String, Object> getLoanData(final Investment i, final Loan l) {
        final Money returned = getReturned(i);
        final Money originalPrincipal = i.getPurchasePrice();
        final Money balance = i.getSmpSoldFor()
                .map(soldFor -> {
                    final Money in = soldFor.add(returned);
                    final Money fee = i.getSmpFee().orElse(soldFor.getZero());
                    final Money out = originalPrincipal.add(fee);
                    return in.subtract(out);
                }).orElseGet(() -> returned.subtract(originalPrincipal));
        final Map<String, Object> loanData = new HashMap<>(getLoanData(l));
        loanData.put("investedOn", toDate(i.getInvestmentDate()));
        loanData.put("loanTermRemaining", i.getRemainingMonths());
        loanData.put("amountRemaining", i.getRemainingPrincipal().orElse(Money.ZERO).getValue());
        loanData.put("amountHeld", originalPrincipal.getValue());
        loanData.put("amountPaid", returned.getValue());
        loanData.put("balance", balance.getValue());
        loanData.put("interestExpected", i.getExpectedInterest().getValue());
        loanData.put("principalPaid", i.getPaidPrincipal().getValue());
        loanData.put("interestPaid", i.getPaidInterest().getValue());
        loanData.put("penaltiesPaid", i.getPaidPenalty().getValue());
        loanData.put("monthsElapsed", getMonthsElapsed(i));
        loanData.put("insurance", i.isInsuranceActive()); // override the one coming from parent
        loanData.put("postponed", i.isInstalmentPostponement());
        return loanData;
    }

    private static Map<String, Object> moneyPerRating(final Function<Rating, Money> provider) {
        final Function<Rating, Number> converter = m -> provider.apply(m).getValue();
        return Stream.of(Rating.values()).collect(Collectors.toMap(Rating::getCode, converter));
    }

    private static Map<String, Object> numberPerRating(final Function<Rating, Number> provider) {
        return Stream.of(Rating.values()).collect(Collectors.toMap(Rating::getCode, provider));
    }

    public static Map<String, Object> summarizePortfolioStructure(final PortfolioOverview portfolioOverview) {
        return Map.ofEntries(
                entry("absoluteShare", moneyPerRating(portfolioOverview::getInvested)),
                entry("relativeShare", numberPerRating(portfolioOverview::getShareOnInvestment)),
                entry("total", portfolioOverview.getInvested().getValue()),
                entry("profitability", portfolioOverview.getAnnualProfitability()),
                entry("monthlyProfit", portfolioOverview.getMonthlyProfit().getValue()),
                entry("minimalProfitability", portfolioOverview.getMinimalAnnualProfitability()),
                entry("minimalMonthlyProfit", portfolioOverview.getMinimalMonthlyProfit().getValue()),
                entry("optimalProfitability", portfolioOverview.getOptimalAnnualProfitability()),
                entry("optimalMonthlyProfit", portfolioOverview.getOptimalMonthlyProfit().getValue()),
                entry("timestamp", toDate(portfolioOverview.getTimestamp()))
        );
    }

    public static Map<String, Object> summarizePortfolioStructure(final ExtendedPortfolioOverview portfolioOverview) {
        final Map<String, Object> entries =
                new LinkedHashMap<>(summarizePortfolioStructure((PortfolioOverview) portfolioOverview));
        entries.putAll(Map.ofEntries(
                entry("absoluteRisk", moneyPerRating(portfolioOverview::getAtRisk)),
                entry("relativeRisk", numberPerRating(portfolioOverview::getAtRiskShareOnInvestment)),
                entry("absoluteSellable", moneyPerRating(portfolioOverview::getSellable)),
                entry("relativeSellable", numberPerRating(portfolioOverview::getShareSellable)),
                entry("absoluteSellableFeeless", moneyPerRating(portfolioOverview::getSellableFeeless)),
                entry("relativeSellableFeeless", numberPerRating(portfolioOverview::getShareSellableFeeless)),
                entry("totalRisk", portfolioOverview.getAtRisk().getValue()),
                entry("totalSellable", portfolioOverview.getSellable().getValue()),
                entry("totalSellableFeeless", portfolioOverview.getSellableFeeless().getValue()),
                entry("totalShare", portfolioOverview.getShareAtRisk()),
                entry("totalSellableShare", portfolioOverview.getShareSellable()),
                entry("totalSellableFeelessShare", portfolioOverview.getShareSellableFeeless()),
                entry("timestamp", toDate(portfolioOverview.getTimestamp()))
        ));
        return entries;
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

    private static Money getReturned(final Investment i) {
        return i.getPaidInterest()
                .add(i.getPaidPrincipal())
                .add(i.getPaidPenalty());
    }

    private static long getMonthsElapsed(final Investment i) {
        return Period.between(i.getInvestmentDate().toLocalDate(), DateUtil.localNow().toLocalDate()).toTotalMonths();
    }

    public static Map<String, Object> getDelinquentData(final Investment i, final Loan loan, final LocalDate date) {
        final Map<String, Object> result = new HashMap<>(getLoanData(i, loan));
        result.put("since", toDate(date));
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
