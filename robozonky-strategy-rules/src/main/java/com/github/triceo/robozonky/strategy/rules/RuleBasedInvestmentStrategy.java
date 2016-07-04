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

package com.github.triceo.robozonky.strategy.rules;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import com.github.triceo.robozonky.strategy.rules.facts.AcceptedLoan;
import com.github.triceo.robozonky.strategy.rules.facts.ProposedLoan;
import com.github.triceo.robozonky.strategy.rules.facts.RatingShare;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RuleBasedInvestmentStrategy implements InvestmentStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBasedInvestmentStrategy.class);

    private static Loan matchLoan(final AcceptedLoan l, final List<Loan> loans) {
        for (final Loan loan: loans) {
            if (loan.getId() == l.getId()) {
                return loan;
            }
        }
        throw new IllegalStateException("Could not find matching loan. Should not have happened.");
    }

    private final KieContainer kieContainer;
    private final Map<Loan, Integer> recommendedAmounts = new HashMap<>();

    public RuleBasedInvestmentStrategy(final KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    @Override
    public synchronized List<Loan> getMatchingLoans(final List<Loan> availableLoans,
                                                    final Map<Rating, BigDecimal> shareOfRatings,
                                                    final BigDecimal availableBalance) {
        this.recommendedAmounts.clear();
        RuleBasedInvestmentStrategy.LOGGER.trace("Started matching loans.");
        final KieSession session = this.kieContainer.newKieSession();
        // insert facts into session
        availableLoans.stream().map(ProposedLoan::new).forEach(session::insert);
        shareOfRatings.entrySet().stream()
                .map(e -> new RatingShare(e.getKey(), e.getValue()))
                .forEach(session::insert);
        // reason over facts and process results
        RuleBasedInvestmentStrategy.LOGGER.trace("Facts inserted into session.");
        session.fireAllRules();
        RuleBasedInvestmentStrategy.LOGGER.trace("Drools finished.");
        final Collection<AcceptedLoan> result =
                (Collection<AcceptedLoan>)session.getObjects(o -> o instanceof AcceptedLoan);
        session.dispose();
        RuleBasedInvestmentStrategy.LOGGER.trace("Session disposed.");
        final Map<AcceptedLoan, Loan> map = result.stream()
                .collect(Collectors.toMap(Function.identity(),
                        l -> RuleBasedInvestmentStrategy.matchLoan(l, availableLoans)));
        map.forEach((fake, actual) -> this.recommendedAmounts.put(actual, fake.getAmount()));
        RuleBasedInvestmentStrategy.LOGGER.trace("Found recommended amounts.");
        // return results in the order of decreasing priority
        final List<Loan> finalResult = result.stream()
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .map(map::get)
                .collect(Collectors.toList());
        RuleBasedInvestmentStrategy.LOGGER.trace("Loans matched.");
        return finalResult;
    }

    @Override
    public synchronized int recommendInvestmentAmount(final Loan loan, final Map<Rating, BigDecimal> ratingShare,
                                                      final BigDecimal balance) {
        final Integer result = this.recommendedAmounts.get(loan);
        return (result == null) ? 0 : result;
    }

}
