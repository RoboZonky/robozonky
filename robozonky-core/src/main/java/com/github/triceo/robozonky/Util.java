/*
 * Copyright 2016 Lukáš Petrovický
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
package com.github.triceo.robozonky;

import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Loan;

import static java.util.Comparator.comparing;

public class Util {

    static List<Loan> sortLoansByTerm(final Collection<Loan> loans, final boolean longestFirst) {
        final List<Loan> sortedLoans = new ArrayList<>(new HashSet<>(loans)); // unique loans
        Comparator<Loan> loanComparator = comparing(Loan::getTermInMonths);
        if (longestFirst) {
            loanComparator = loanComparator.reversed();
        }
        Collections.sort(sortedLoans, loanComparator);
        return Collections.unmodifiableList(sortedLoans);
    }

    /**
     * Determine whether or not a given loan is present among existing investments.
     *
     * @param loan Loan in question.
     * @param investments Known investments.
     * @return True if present.
     */
    static boolean isLoanPresent(final Loan loan, final Iterable<Investment> investments) {
        for (final Investment i : investments) {
            if (loan.getId() == i.getLoanId()) {
                return true;
            }
        }
        return false;
    }

    static BigDecimal sum(final Collection<BigDecimal> vals) {
        return vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static Collection<Investment> mergeInvestments(final Collection<Investment> left,
                                                          final Collection<Investment> right) {
        if (left.size() == 0) {
            return Collections.unmodifiableCollection(right);
        } else if (right.size() == 0) {
            return Collections.unmodifiableCollection(left);
        } else {
            final Map<Integer, Investment> investments
                    = left.stream().collect(Collectors.toMap(Investment::getLoanId, Function.identity()));
            right.stream().filter(investment -> !investments.containsKey(investment.getLoanId()))
                    .forEach(investment -> investments.put(investment.getLoanId(), investment));
            return Collections.unmodifiableCollection(investments.values());
        }
    }

    public static String getRoboZonkyVersion() {
        try {
            final URLClassLoader cl = (URLClassLoader) Operations.class.getClassLoader();
            final URL url = cl.findResource("META-INF/maven/com.github.triceo.robozonky/robozonky-core/pom.properties");
            final Properties props = new Properties();
            props.load(url.openStream());
            return props.getProperty("version", Operations.ZONKY_VERSION_UNKNOWN);
        } catch (Exception ex) {
            return Operations.ZONKY_VERSION_UNDETECTED;
        }
    }

}
