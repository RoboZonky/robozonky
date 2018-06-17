/*
 * Copyright 2018 The RoboZonky Project
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.LoanBased;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.api.Defaults;

class Util {

    private static final String AT = "@";
    private static final Pattern COMPILE = Pattern.compile("\\Q" + AT + "\\E");

    private static Date toDate(final LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(Defaults.ZONE_ID).toInstant());
    }

    private static Date toDate(final OffsetDateTime offsetDateTime) {
        return Date.from(offsetDateTime.toInstant());
    }

    public static String identifyLoan(final LoanBased event) {
        final Loan loan = event.getLoan();
        return "č. " + loan.getId() + " (" + loan.getRating().getCode() + ", " + loan.getTermInMonths() + " m.)";
    }

    public static Map<String, Object> getLoanData(final Loan loan) {
        return new HashMap() {{
            put("loanId", loan.getId());
            put("loanAmount", loan.getAmount());
            put("loanRating", loan.getRating().getCode());
            put("loanTerm", loan.getTermInMonths());
            put("loanUrl", loan.getUrl());
            put("loanRegion", loan.getRegion());
            put("loanMainIncomeType", loan.getMainIncomeType());
            put("loanPurpose", loan.getPurpose());
            put("loanName", loan.getName());
            put("insurance", loan.isInsuranceActive());
        }};
    }

    private static Map<String, Object> perRating(final Function<Rating, Number> provider) {
        return Stream.of(Rating.values()).collect(Collectors.toMap(Rating::getCode, provider::apply));
    }

    public static Map<String, Object> summarizePortfolioStructure(final PortfolioOverview portfolioOverview) {
        return Collections.unmodifiableMap(new HashMap() {{
            put("absoluteShare", perRating(portfolioOverview::getCzkInvested));
            put("relativeShare", perRating(portfolioOverview::getShareOnInvestment));
            put("absoluteRisk", perRating(portfolioOverview::getCzkAtRisk));
            put("relativeRisk", perRating(portfolioOverview::getAtRiskShareOnInvestment));
            put("total", portfolioOverview.getCzkInvested());
            put("totalRisk", portfolioOverview.getCzkAtRisk());
            put("totalShare", portfolioOverview.getShareAtRisk());
            put("balance", portfolioOverview.getCzkAvailable());
        }});
    }

    private static BigDecimal getTotalPaid(final Investment i) {
        return i.getPaidInterest().add(i.getPaidPrincipal()).add(i.getPaidPenalty());
    }

    private static long getMonthsElapsed(final Investment i) {
        return i.getInvestmentDate()
                .map(d -> Period.between(d.toLocalDate(), LocalDate.now()).toTotalMonths())
                .orElse((long) (i.getOriginalTerm() - i.getCurrentTerm() + 1));
    }

    public static Map<String, Object> getLoanData(final Investment i, final Loan l) {
        final Map<String, Object> loanData = getLoanData(l);
        loanData.put("loanTermRemaining", i.getRemainingMonths());
        loanData.put("amountRemaining", i.getRemainingPrincipal());
        loanData.put("amountHeld", i.getOriginalPrincipal());
        loanData.put("amountPaid", getTotalPaid(i));
        loanData.put("monthsElapsed", getMonthsElapsed(i));
        loanData.put("postponed", i.areInstalmentsPostponed());
        return loanData;
    }

    public static Map<String, Object> getDelinquentData(final Investment i, final Loan loan,
                                                        final Collection<Development> collections,
                                                        final LocalDate date) {
        final Map<String, Object> result = getLoanData(i, loan);
        result.put("since", Util.toDate(date));
        result.put("collections", collections.stream()
                .sorted(Comparator.comparing(Development::getDateFrom).reversed())
                .limit(5)
                .map(action -> new HashMap<String, Object>() {{
                    put("code", action.getType().getCode());
                    put("note", action.getPublicNote().orElse("Bez dalšího vysvětlení."));
                    put("startDate", Util.toDate(action.getDateFrom()));
                    put("endDate", action.getDateTo().map(
                            Util::toDate).orElse(null));
                }}).collect(Collectors.toList()));
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
