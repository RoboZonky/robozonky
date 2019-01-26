/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.cli;

import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.confirmations.RequestId;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.api.remote.entities.RawLoan;
import com.github.robozonky.common.extensions.ListenerServiceLoader;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.internal.util.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Checker {

    private static final Logger LOGGER = LogManager.getLogger(Checker.class);
    private static final Comparator<RawLoan> SUBCOMPARATOR =
            Comparator.comparing(RawLoan::getRemainingInvestment).reversed();
    private static final Comparator<RawLoan> COMPARATOR =
            Comparator.comparing(RawLoan::getInterestRate).thenComparing(SUBCOMPARATOR);

    private Checker() {
        // no instances
    }

    private static Optional<RawLoan> getOneLoanFromMarketplace(final Supplier<ApiProvider> apiProviderSupplier) {
        try {
            final ApiProvider p = apiProviderSupplier.get();
            final Collection<RawLoan> loans = p.marketplace();
            /*
             * find a loan that is likely to stay on the marketplace for so long that the notification will
             * successfully come through.
             */
            return loans.stream().min(COMPARATOR);
        } catch (final Exception t) {
            LOGGER.warn("Failed obtaining a loan.", t);
            return Optional.empty();
        }
    }

    private static boolean notifyProvider(final RawLoan loan, final ConfirmationProvider zonkoid, final String username,
                                          final char... secret) {
        final RequestId id = new RequestId(username, secret);
        return zonkoid.requestConfirmation(id, loan.getId(), 200);
    }

    public static boolean confirmations(final ConfirmationProvider provider, final String username,
                                        final char... secret) {
        return confirmations(provider, username, secret, ApiProvider::new);
    }

    static boolean confirmations(final ConfirmationProvider provider, final String username, final char[] secret,
                                 final Supplier<ApiProvider> apiProviderSupplier) {
        return getOneLoanFromMarketplace(apiProviderSupplier)
                .map(l -> notifyProvider(l, provider, username, secret))
                .orElse(false);
    }

    static boolean notifications(final SessionInfo sessionInfo, final URL configurationLocation) {
        ListenerServiceLoader.registerConfiguration(sessionInfo, configurationLocation);
        return notifications(sessionInfo, ListenerServiceLoader.load(RoboZonkyTestingEvent.class));
    }

    static boolean notifications(final SessionInfo sessionInfo,
                                 final List<EventListenerSupplier<RoboZonkyTestingEvent>> refreshables) {
        final Collection<EventListener<RoboZonkyTestingEvent>> listeners = refreshables.stream()
                .flatMap(r -> r.get().map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toSet());
        if (listeners.isEmpty()) {
            return false;
        } else {
            final RoboZonkyTestingEvent evt = DateUtil::offsetNow;
            listeners.forEach(l -> l.handle(evt, sessionInfo));
            return true;
        }
    }
}
