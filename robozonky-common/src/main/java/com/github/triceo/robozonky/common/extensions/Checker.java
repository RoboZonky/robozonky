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

package com.github.triceo.robozonky.common.extensions;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.confirmations.RequestId;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.triceo.robozonky.api.notifications.SessionInfo;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Checker {

    private static final Logger LOGGER = LoggerFactory.getLogger(Checker.class);
    private static final Comparator<Loan> SUBCOMPARATOR =
            Comparator.comparing(Loan::getRemainingInvestment).reversed();
    private static final Comparator<Loan> COMPARATOR =
            Comparator.comparing(Loan::getInterestRate).thenComparing(Checker.SUBCOMPARATOR);

    static Optional<Loan> getOneLoanFromMarketplace(final Supplier<ApiProvider> apiProviderSupplier) {
        try {
            final ApiProvider p = apiProviderSupplier.get();
            final Collection<Loan> loans = p.marketplace();
            /*
             * find a loan that is likely to stay on the marketplace for so long that the notification will
             * successfully come through.
             */
            return loans.stream().sorted(Checker.COMPARATOR).findFirst();
        } catch (final Exception t) {
            LOGGER.warn("Failed obtaining a loan.", t);
            return Optional.empty();
        }
    }

    static Optional<Boolean> notifyProvider(final Loan loan, final ConfirmationProvider zonkoid, final String username,
                                            final char[] secret) {
        final RequestId id = new RequestId(username, secret);
        return zonkoid.requestConfirmation(id, loan.getId(), 200)
                .map(c -> {
                    switch (c.getType()) {
                        case APPROVED:
                        case DELEGATED:
                            return Optional.of(true);
                        default:
                            return Optional.of(false);
                    }
                }).orElse(Optional.empty());
    }

    public static Optional<Boolean> confirmations(final ConfirmationProvider provider, final String username,
                                                  final char[] secret) {
        return Checker.confirmations(provider, username, secret, ApiProvider::new);
    }

    static Optional<Boolean> confirmations(final ConfirmationProvider provider, final String username,
                                           final char[] secret, final Supplier<ApiProvider> apiProviderSupplier) {
        return Checker.getOneLoanFromMarketplace(apiProviderSupplier)
                .map(l -> Checker.notifyProvider(l, provider, username, secret))
                .orElse(Optional.of(false));
    }


    public static boolean notifications(final String username) {
        return Checker.notifications(username, ListenerServiceLoader.load(RoboZonkyTestingEvent.class));
    }

    public static boolean notifications(final String username,
                                        final List<Refreshable<EventListener<RoboZonkyTestingEvent>>> refreshables) {
        final Collection<EventListener<RoboZonkyTestingEvent>> listeners = refreshables.stream()
                        .flatMap(r -> r.getLatest().map(Stream::of).orElse(Stream.empty()))
                        .collect(Collectors.toSet());
        if (listeners.size() > 0) {
            final SessionInfo sessionInfo = new SessionInfo(username);
            final RoboZonkyTestingEvent evt = new RoboZonkyTestingEvent();
            listeners.forEach(l -> l.handle(evt, sessionInfo));
            return true;
        } else {
            return false;
        }
    }

}
