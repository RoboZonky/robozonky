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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.triceo.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatus;
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

    static void sendEvents(final Collection<Delinquent> delinquents, final Zonky zonky,
                           final BiFunction<Loan, Delinquent, Event> eventSupplier) {
        delinquents.forEach(d -> {
            final Loan l = zonky.getLoan(d.getLoanId());
            Events.fire(eventSupplier.apply(l, d));
        });
    }

    @Override
    public void accept(final Zonky zonky) {
        LOGGER.info("Daily update started.");
        final Map<PaymentStatus, List<Investment>> investments = zonky.getInvestments()
                .collect(Collectors.groupingBy(Investment::getPaymentStatus));
        final PresentDelinquents d = new PresentDelinquents();
        final Collection<Delinquent> before = d.get();
        d.update(investments.get(PaymentStatus.DUE));
        final Collection<Delinquent> after = d.get();
        DelinquencyUpdate.sendEvents(DelinquencyUpdate.getDifference(after, before), zonky,
                                     (l, i) -> new LoanDelinquentEvent(l, i.getSince()));
        DelinquencyUpdate.sendEvents(DelinquencyUpdate.getDifference(before, after), zonky,
                                     (l, i) -> new LoanNoLongerDelinquentEvent(l));
        Stream.of(DelinquencyCategory.values()).forEach(c -> {
            LOGGER.debug("Updating {}.", c);
            c.updateKnownDelinquents(after);
            c.purge(investments.get(PaymentStatus.PAID));
        });
        LOGGER.debug("Finished.");
    }
}
