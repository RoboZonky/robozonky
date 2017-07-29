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

package com.github.triceo.robozonky.app.delinquency;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatus;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatuses;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelinquencyUpdate implements Consumer<Zonky> {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            DelinquencyUpdate.class);

    static Collection<Investment> getWithPaymentStatus(final Map<PaymentStatus, List<Investment>> investments,
                                                       final PaymentStatuses target) {
        return target.getPaymentStatuses().stream()
                .flatMap(ps -> investments.get(ps).stream())
                .collect(Collectors.toSet());
    }

    @Override
    public void accept(final Zonky zonky) {
        LOGGER.info("Daily update started.");
        final Map<PaymentStatus, List<Investment>> investments = zonky.getInvestments()
                .collect(Collectors.groupingBy(Investment::getPaymentStatus));
        DelinquencyTracker.INSTANCE.update(zonky, getWithPaymentStatus(investments, PaymentStatus.getDelinquent()),
                                           getWithPaymentStatus(investments, PaymentStatus.getDone()));
        LOGGER.debug("Finished.");
    }
}
