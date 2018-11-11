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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.util.BigDecimalCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;

final class AmountsAtRiskSummarizer implements PortfolioDependant {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmountsAtRiskSummarizer.class);

    @Override
    public void accept(final TransactionalPortfolio transactional) {
        final Tenant tenant = transactional.getTenant();
        final Map<Rating, BigDecimal> summarized = tenant.call(Zonky::getDelinquentInvestments)
                .parallel() // possibly many pages' worth of results; fetch in parallel
                .collect(groupingBy(Investment::getRating,
                                    () -> new EnumMap<>(Rating.class),
                                    mapping(i -> {
                                        final BigDecimal principalNotYetReturned = i.getRemainingPrincipal()
                                                .subtract(i.getPaidInterest())
                                                .subtract(i.getPaidPenalty())
                                                .max(BigDecimal.ZERO);
                                        LOGGER.debug("Delinquent: {} CZK in loan #{}, investment #{}.",
                                                     principalNotYetReturned, i.getLoanId(), i.getId());
                                        return principalNotYetReturned;
                                    }, reducing(BigDecimal.ZERO, BigDecimalCalculator::plus))));
        transactional.getPortfolio().amountsAtRiskUpdated(summarized);
    }
}
