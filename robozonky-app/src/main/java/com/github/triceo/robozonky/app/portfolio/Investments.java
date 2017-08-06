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

package com.github.triceo.robozonky.app.portfolio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.enums.InvestmentStatus;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatus;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatuses;
import com.github.triceo.robozonky.app.util.DaemonRuntimeExceptionHandler;
import com.github.triceo.robozonky.common.remote.Zonky;

public enum Investments {

    INSTANCE;

    private final AtomicReference<List<Investment>> investments = new AtomicReference<>(Collections.emptyList());
    private final Collection<Consumer<Zonky>> updaters = new CopyOnWriteArraySet<>();

    public void registerUpdater(final Consumer<Zonky> updater) {
        updaters.add(updater);
    }

    public void update(final Zonky zonky, final Collection<Investment> allInvestments) {
        try {
            investments.set(new ArrayList<>(allInvestments));
            updaters.forEach(u -> u.accept(zonky));
        } catch (final Throwable t) {
            new DaemonRuntimeExceptionHandler().handle(t);
        }
    }

    public Stream<Investment> getWithPaymentStatus(final Set<PaymentStatus> statuses) {
        return investments.get().stream()
                .filter(i -> i.getStatus() != InvestmentStatus.SOLD) // sold investments have no payment status
                .filter(i -> statuses.stream().anyMatch(s -> Objects.equals(s, i.getPaymentStatus())));
    }

    public Stream<Investment> getWithPaymentStatus(final PaymentStatuses statuses) {
        return getWithPaymentStatus(statuses.getPaymentStatuses());
    }

}
