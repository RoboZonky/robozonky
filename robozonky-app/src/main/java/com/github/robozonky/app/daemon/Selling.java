/*
 * Copyright 2020 The RoboZonky Project
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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.LoanHealthInfo;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.tenant.Tenant;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.app.events.impl.EventFactory.sellingCompletedLazy;

/**
 * Implements selling of {@link Investment}s on the secondary marketplace.
 */
final class Selling implements TenantPayload {

    private static final Logger LOGGER = Audit.selling();

    private static Optional<Investment> processSale(final PowerTenant tenant, final RecommendedInvestment r,
                                                    final SoldParticipationCache sold) {
        final Investment i = r.descriptor().item();
        final boolean isRealRun = !tenant.getSessionInfo().isDryRun();
        LOGGER.debug("Will send sell request for loan #{}: {}.", i.getLoanId(), isRealRun);
        if (isRealRun) {
            tenant.run(z -> z.sell(i));
            LOGGER.info("Offered to sell investment in loan #{}.", i.getLoanId());
        }
        sold.markAsOffered(i.getLoanId());
        tenant.fire(EventFactory.saleOffered(i, r.descriptor().related()));
        return Optional.of(i);
    }

    private static void sell(final PowerTenant tenant, final SellStrategy strategy) {
        final Select sellable = Select.sellableParticipations();
        final SoldParticipationCache sold = SoldParticipationCache.forTenant(tenant);
        final Set<InvestmentDescriptor> eligible = tenant.call(zonky -> zonky.getInvestments(sellable))
                .filter(i -> sold.getOffered().noneMatch(id -> id == i.getLoanId())) // to enable dry run
                .filter(i -> !sold.wasOnceSold(i.getLoanId()))
                .map(i -> {
                    Supplier<Loan> loanSupplier = () -> tenant.getLoan(i.getLoanId());
                    LoanHealthInfo healthInfo = i.getLoanHealthInfo().orElse(LoanHealthInfo.UNKNOWN);
                    switch (healthInfo) {
                        case HEALTHY:
                            return new InvestmentDescriptor(i, loanSupplier);
                        case HISTORICALLY_IN_DUE: // Additional sell info is available for possible future use.
                            return new InvestmentDescriptor(i, loanSupplier, () -> tenant.getSellInfo(i.getId()));
                        default:
                            throw new IllegalStateException("Unsupported loan status: " + healthInfo);
                    }
                }).collect(Collectors.toSet());
        final PortfolioOverview overview = tenant.getPortfolio().getOverview();
        tenant.fire(EventFactory.sellingStarted(eligible, overview));
        final Stream<RecommendedInvestment> recommended = strategy.recommend(eligible, overview)
                .peek(r -> tenant.fire(EventFactory.saleRecommended(r)));
        final Stream<RecommendedInvestment> throttled = new SellingThrottle().apply(recommended, overview);
        final Collection<Investment> investmentsSold = throttled
                .map(r -> processSale(tenant, r, sold))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        tenant.fire(sellingCompletedLazy(() -> EventFactory.sellingCompleted(investmentsSold,
                                                                             tenant.getPortfolio().getOverview())));
    }

    @Override
    public void accept(final Tenant tenant) {
        tenant.getSellStrategy().ifPresent(s -> sell((PowerTenant) tenant, s));
    }
}
