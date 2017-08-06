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

import java.util.Collections;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class CheckerTest {

    @Test
    public void confirmationsMarketplaceFail() {
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        Mockito.doThrow(new IllegalStateException("Testing")).when(provider).marketplace();
        final boolean result =
                Checker.confirmations(Mockito.mock(ConfirmationProvider.class), "", new char[0], () -> provider);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void confirmationsMarketplaceWithoutLoans() {
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        Mockito.when(provider.marketplace()).thenReturn(Collections.emptyList());
        final boolean result =
                Checker.confirmations(Mockito.mock(ConfirmationProvider.class), "", new char[0], () -> provider);
        Assertions.assertThat(result).isFalse();
    }

    private static ApiProvider mockApiThatReturnsOneLoan() {
        final Loan l = Mockito.mock(Loan.class);
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        Mockito.doReturn(Collections.singletonList(l)).when(provider).marketplace();
        return provider;
    }

    @Test
    public void confirmationsNotConfirming() {
        final ConfirmationProvider cp = Mockito.mock(ConfirmationProvider.class);
        Mockito.when(cp.requestConfirmation(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                            ArgumentMatchers.anyInt())).thenReturn(false);
        final boolean result = Checker.confirmations(cp, "", new char[0], () -> mockApiThatReturnsOneLoan());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void confirmationsRejecting() {
        final ConfirmationProvider cp = Mockito.mock(ConfirmationProvider.class);
        Mockito.when(cp.requestConfirmation(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                            ArgumentMatchers.anyInt())).thenReturn(false);
        final boolean result = Checker.confirmations(cp, "", new char[0], () -> mockApiThatReturnsOneLoan());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void confirmationsProper() {
        final ConfirmationProvider cp = Mockito.mock(ConfirmationProvider.class);
        Mockito.when(cp.requestConfirmation(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                            ArgumentMatchers.anyInt())).thenReturn(false);
        final boolean result = Checker.confirmations(cp, "", new char[0], () -> mockApiThatReturnsOneLoan());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void notificationsEmptyOnInput() {
        Assertions.assertThat(Checker.notifications("", Collections.emptyList())).isFalse();
    }

    @Test
    public void notificationsEmptyWhenRefreshed() {
        final Refreshable<EventListener<RoboZonkyTestingEvent>> r = Refreshable.createImmutable(null);
        r.run();
        Assertions.assertThat(Checker.notifications("", Collections.singletonList(r))).isFalse();
    }

    @Test
    public void notificationsProper() {
        final EventListener<RoboZonkyTestingEvent> l = Mockito.mock(EventListener.class);
        final Refreshable<EventListener<RoboZonkyTestingEvent>> r = Refreshable.createImmutable(l);
        r.run();
        Assertions.assertThat(Checker.notifications("", Collections.singletonList(r))).isTrue();
        Mockito.verify(l).handle(ArgumentMatchers.any(RoboZonkyTestingEvent.class), ArgumentMatchers.any());
    }
}

