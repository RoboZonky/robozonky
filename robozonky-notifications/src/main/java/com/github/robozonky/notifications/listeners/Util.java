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
import com.github.robozonky.internal.util.DateUtil;
import com.github.robozonky.internal.util.Maps;

import static com.github.robozonky.internal.util.Maps.entry;

final class Util {

    private static final BigDecimal HUNDRED = BigDecimal.TEN.pow(2);
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
        final BigDecimal interestRate = loan.getInterestRate().multiply(HUNDRED);
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
        return Maps.ofEntries(
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
        return Maps.ofEntries(
                entry("absoluteShare", perRating(portfolioOverview::getCzkInvested)),
                entry("relativeShare", perRating(portfolioOverview::getShareOnInvestment)),
                entry("absoluteRisk", perRating(portfolioOverview::getCzkAtRisk)),
                entry("relativeRisk", perRating(portfolioOverview::getAtRiskShareOnInvestment)),
                entry("total", portfolioOverview.getCzkInvested()),
                entry("totalRisk", portfolioOverview.getCzkAtRisk()),
                entry("totalShare", portfolioOverview.getShareAtRisk()),
                entry("balance", portfolioOverview.getCzkAvailable()),
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
        return i.getPaidInterest().add(i.getPaidPrincipal()).add(i.getPaidPenalty());
    }

    private static long getMonthsElapsed(final Investment i) {
        return Period.between(i.getInvestmentDate().toLocalDate(), DateUtil.localNow().toLocalDate()).toTotalMonths();
    }

    public static Map<String, Object> getLoanData(final Investment i, final MarketplaceLoan l) {
        final Map<String, Object> loanData = new HashMap<>(getLoanData(l));
        loanData.put("investedOn", Util.toDate(i.getInvestmentDate()));
        loanData.put("loanTermRemaining", i.getRemainingMonths());
        loanData.put("amountRemaining", i.getRemainingPrincipal());
        loanData.put("amountHeld", i.getOriginalPrincipal());
        loanData.put("amountPaid", getTotalPaid(i));
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
                .map(action -> Maps.ofEntries(
                        entry("code", action.getType().getCode()),
                        entry("note", action.getPublicNote().orElse("Bez dalšího vysvětlení.")),
                        entry("startDate", Util.toDate(action.getDateFrom())),
                        entry("endDate", action.getDateTo().map(Util::toDate).orElse(null))
                )).collect(Collectors.toList()));
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
