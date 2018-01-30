/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.notifications.email;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.util.InvestmentInference;

class Util {

    private static final String AT = "@";
    private static final Pattern COMPILE = Pattern.compile("\\Q" + AT + "\\E");

    private static Date toDate(final LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(Defaults.ZONE_ID).toInstant());
    }

    public static Map<String, Object> getLoanData(final Loan loan) {
        return new HashMap<String, Object>() {{
            put("loanId", loan.getId());
            put("loanAmount", loan.getAmount());
            put("loanRating", loan.getRating().getCode());
            put("loanTerm", loan.getTermInMonths());
            put("loanUrl", Loan.getUrlSafe(loan));
            put("loanRegion", loan.getRegion());
            put("loanMainIncomeType", loan.getMainIncomeType());
            put("loanPurpose", loan.getPurpose());
            put("loanName", loan.getName());
        }};
    }

    public static Map<String, Object> summarizePortfolioStructure(final PortfolioOverview portfolioOverview) {
        final Map<String, Object> result = new HashMap<>(3);
        result.put("absoluteShare", new LinkedHashMap<String, Object>() {{
            Stream.of(Rating.values()).forEach(r -> put(r.getCode(), portfolioOverview.getCzkInvested(r)));
        }});
        result.put("relativeShare", new LinkedHashMap<String, Object>() {{
            Stream.of(Rating.values()).forEach(r -> put(r.getCode(), portfolioOverview.getShareOnInvestment(r)));
        }});
        result.put("total", portfolioOverview.getCzkInvested());
        result.put("balance", portfolioOverview.getCzkAvailable());
        for (final Rating rating : Rating.values()) {
            final Map<String, Object> map = (Map<String, Object>) result.get("absoluteShare");
            map.put(rating.getCode(), portfolioOverview.getCzkInvested(rating));
            final Map<String, Object> map2 = (Map<String, Object>) result.get("relativeShare");
            map2.put(rating.getCode(), portfolioOverview.getShareOnInvestment(rating));
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> getLoanData(final Investment i, final Loan l) {
        final InvestmentInference infered = InvestmentInference.with(i, l);
        final Map<String, Object> loanData = getLoanData(l);
        loanData.put("loanTermRemaining", i.getRemainingMonths());
        loanData.put("amountRemaining", i.getRemainingPrincipal());
        loanData.put("amountHeld", infered.getOriginalAmount());
        loanData.put("amountPaid", infered.getTotalAmountPaid());
        loanData.put("monthsElapsed", infered.getElapsed(LocalDate.now()).toTotalMonths());
        return loanData;
    }

    public static Map<String, Object> getDelinquentData(final Investment i, final Loan loan, final LocalDate date) {
        final Map<String, Object> result = getLoanData(i, loan);
        result.put("since", Util.toDate(date));
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
