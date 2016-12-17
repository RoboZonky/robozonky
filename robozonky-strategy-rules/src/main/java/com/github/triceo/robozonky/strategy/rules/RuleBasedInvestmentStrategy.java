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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.Recommendation;
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
 */
class RuleBasedInvestmentStrategy implements InvestmentStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBasedInvestmentStrategy.class);

    private static LoanDescriptor matchLoan(final AcceptedLoan l, final Collection<LoanDescriptor> loans) {
        return loans.stream()
                .filter(ld -> ld.getLoan().getId() == l.getId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find matching loan. Technically impossible."));
    }

    private final KieContainer kieContainer;

    RuleBasedInvestmentStrategy(final KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Recommendation> recommend(final Collection<LoanDescriptor> availableLoans,
                                          final PortfolioOverview portfolio) {
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
        final Map<AcceptedLoan, LoanDescriptor> map = result.stream()
                .collect(Collectors.toMap(Function.identity(),
                        l -> RuleBasedInvestmentStrategy.matchLoan(l, availableLoans)));
        final List<Recommendation> recommendations = map.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(e -> e.getValue().recommend(e.getKey().getAmount(), e.getKey().isConfirmationRequired()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        RuleBasedInvestmentStrategy.LOGGER.trace("Loans matched.");
        return Collections.unmodifiableList(recommendations);
    }

}
