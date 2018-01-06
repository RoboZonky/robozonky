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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.internal.api.Defaults;

class Util {

    private static final String AT = "@";
    private static final Pattern COMPILE = Pattern.compile("\\Q" + AT + "\\E");

    public static Date toDate(final LocalDate localDate) {
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

    public static Map<String, Object> getLoanData(final Investment i, final Loan l) {
        final Map<String, Object> loanData = getLoanData(l);
        loanData.put("loanTermRemaining", i.getRemainingMonths());
        loanData.put("investedAmount", i.getRemainingPrincipal());
        return loanData;
    }

    public static Map<String, Object> getDelinquentData(final Investment i, final LocalDate date) {
        final int loanId = i.getLoanId();
        return new HashMap<String, Object>() {{
            put("loanRating", i.getRating());
            put("loanName", i.getLoanName());
            put("loanId", loanId);
            put("totalAmount", i.getAmount());
            put("remainingAmount", i.getRemainingPrincipal().add(i.getDuePrincipal()));
            put("totalMonths", i.getLoanTermInMonth());
            put("remainingMonths", i.getRemainingMonths());
            put("since", Util.toDate(date));
            put("loanUrl", Loan.guessUrl(loanId));
        }};
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
