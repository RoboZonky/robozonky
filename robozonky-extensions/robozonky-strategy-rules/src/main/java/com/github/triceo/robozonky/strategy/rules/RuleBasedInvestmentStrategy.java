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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This strategy implements evaluation using a Drools decision table. See http://www.drools.org/
 */
class RuleBasedInvestmentStrategy implements InvestmentStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBasedInvestmentStrategy.class);

    private static LoanDescriptor matchLoan(final ProcessedLoan l, final Collection<LoanDescriptor> loans) {
        return loans.stream()
                .filter(ld -> ld.getLoan().getId() == l.getLoan().getId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find matching loan. Technically impossible."));
    }

    private final KieContainer kieContainer;

    RuleBasedInvestmentStrategy(final KieContainer kieContainer) {
        RuleBasedInvestmentStrategy.LOGGER.warn("You are using a deprecated Strategy implementation.");
        RuleBasedInvestmentStrategy.LOGGER.info("Please migrate to robozonky-strategy-natural.");
        this.kieContainer = kieContainer;
    }

    private Stream<ProcessedLoan> processloans(final Collection<LoanDescriptor> loans,
                                               final PortfolioOverview portfolio) {
        RuleBasedInvestmentStrategy.LOGGER.trace("Started matching marketplace.");
        final KieSession session = this.kieContainer.newKieSession();
        try {
            // insert facts into session
            loans.stream().map(LoanDescriptor::getLoan).forEach(session::insert);
            session.insert(portfolio);
            // reason over facts and process results
            RuleBasedInvestmentStrategy.LOGGER.trace("Facts inserted into session.");
            session.fireAllRules();
            RuleBasedInvestmentStrategy.LOGGER.trace("Drools finished.");
            return session.getObjects(o -> o instanceof ProcessedLoan).stream().map(o -> (ProcessedLoan) o);
        } finally { // prevent session leaks in case anything goes wrong
            session.dispose();
            RuleBasedInvestmentStrategy.LOGGER.trace("Session disposed.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<Recommendation> evaluate(final Collection<LoanDescriptor> availableLoans,
                                           final PortfolioOverview portfolio) {
        final Stream<ProcessedLoan> processedLoanStream = processloans(availableLoans, portfolio);
        final Map<ProcessedLoan, LoanDescriptor> map = processedLoanStream.collect(Collectors.toMap(Function.identity(),
                                                                                                    l -> RuleBasedInvestmentStrategy.matchLoan(
                                                                                                            l,
                                                                                                            availableLoans)));
        return map.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey)) // maintain rule-based loan priority
                .map(e -> {
                    final ProcessedLoan ruleBasedLoan = e.getKey();
                    return e.getValue().recommend(ruleBasedLoan.getAmount(), ruleBasedLoan.isConfirmationRequired());
                }).flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
    }

    @Override
    public List<Recommendation> recommend(final Collection<LoanDescriptor> availableLoans,
                                          final PortfolioOverview portfolio) {
        return evaluate(availableLoans, portfolio).collect(Collectors.toList());
    }
}
