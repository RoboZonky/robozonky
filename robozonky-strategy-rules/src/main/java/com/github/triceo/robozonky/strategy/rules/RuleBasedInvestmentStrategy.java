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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.strategy.rules.facts.AcceptedLoan;
import com.github.triceo.robozonky.strategy.rules.facts.ProposedLoan;
import com.github.triceo.robozonky.strategy.rules.facts.RatingShare;
import com.github.triceo.robozonky.strategy.rules.facts.Wallet;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This strategy implements evaluation using a Drools decision table. See http://www.drools.org/
 *
 * Before {@link #recommendInvestmentAmount(Loan, PortfolioOverview)}, you must call
 * {@link #getMatchingLoans(List, PortfolioOverview)}. Otherwise the strategy does not have the decision data.
 */
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

    RuleBasedInvestmentStrategy(final KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized List<Loan> getMatchingLoans(final List<Loan> availableLoans,
                                                    final PortfolioOverview portfolio) {
        this.recommendedAmounts.clear();
        RuleBasedInvestmentStrategy.LOGGER.trace("Started matching loans.");
        final KieSession session = this.kieContainer.newKieSession();
        // insert facts into session
        availableLoans.stream().map(ProposedLoan::new).forEach(session::insert);
        Arrays.stream(Rating.values())
                .map(r -> new RatingShare(r, portfolio.getShareOnInvestment(r)))
                .forEach(session::insert);
        session.insert(new Wallet(portfolio.getCzkAvailable(), portfolio.getCzkInvested()));
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

    /**
     * Does not actually do anything. Only returns a result that had previously been calculated when
     * {@link #getMatchingLoans(List, PortfolioOverview)} was called.
     *
     * @param loan Loan in question.
     * @param portfolio Aggregation of information as to the user's current portfolio.
     * @return How much should be invested into the loan.
     */
    @Override
    public synchronized int recommendInvestmentAmount(final Loan loan, final PortfolioOverview portfolio) {
        final Integer result = this.recommendedAmounts.get(loan);
        return (result == null) ? 0 : result;
    }

}
