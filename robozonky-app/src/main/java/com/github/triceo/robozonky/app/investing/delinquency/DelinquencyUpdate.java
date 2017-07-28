/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.investing.delinquency;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatus;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatuses;
import com.github.triceo.robozonky.app.Events;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelinquencyUpdate implements Consumer<Zonky> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelinquencyUpdate.class);

    /**
     * Returns difference between the two collections.
     * @param one The original collection.
     * @param two The collection to match against.
     * @return Items from the first collection, which are not present in the other.
     */
    static <T> Collection<T> getDifference(final Collection<T> one, final Collection<T> two) {
        return one.stream().filter(i -> !two.contains(i)).collect(Collectors.toSet());
    }

    static Collection<Investment> getWithPaymentStatus(final Map<PaymentStatus, List<Investment>> investments,
                                                       final PaymentStatuses target) {
        return target.getPaymentStatuses().stream()
                .flatMap(ps -> investments.get(ps).stream())
                .collect(Collectors.toSet());
    }

    static Function<Integer, Loan> getLoanProvider(final Zonky zonky) {
        return zonky::getLoan;
    }

    @Override
    public void accept(final Zonky zonky) {
        LOGGER.info("Daily update started.");
        final Map<PaymentStatus, List<Investment>> investments = zonky.getInvestments()
                .collect(Collectors.groupingBy(Investment::getPaymentStatus));
        final PresentDelinquents d = new PresentDelinquents();
        final Collection<Delinquent> before = d.get();
        d.update(DelinquencyUpdate.getWithPaymentStatus(investments, PaymentStatus.getDelinquent()));
        final Collection<Delinquent> after = d.get();
        final Function<Integer, Loan> loanProvider = getLoanProvider(zonky);
        DelinquencyUpdate.getDifference(before, after).forEach(noLongerDelinquent -> {
            final Loan l = loanProvider.apply(noLongerDelinquent.getLoanId());
            Events.fire(new LoanNoLongerDelinquentEvent(l));
        });
        Stream.of(DelinquencyCategory.values()).forEach(c -> {
            LOGGER.debug("Updating {}.", c);
            c.updateKnownDelinquents(after, loanProvider);
            c.purge(DelinquencyUpdate.getWithPaymentStatus(investments, PaymentStatus.getDone()));
        });
        LOGGER.debug("Finished.");
    }
}
