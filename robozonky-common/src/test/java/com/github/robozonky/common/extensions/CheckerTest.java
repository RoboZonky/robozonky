/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.common.extensions;

import java.util.Collections;
import java.util.Optional;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.api.remote.entities.RawLoan;
import com.github.robozonky.common.remote.ApiProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckerTest {

    private static final char[] SECRET = new char[0];

    @Test
    void confirmationsMarketplaceFail() {
        final ApiProvider provider = mock(ApiProvider.class);
        doThrow(new IllegalStateException("Testing")).when(provider).marketplace();
        final boolean result =
                Checker.confirmations(mock(ConfirmationProvider.class), "", SECRET, () -> provider);
        assertThat(result).isFalse();
    }

    @Test
    void confirmationsMarketplaceWithoutLoans() {
        final ApiProvider provider = mock(ApiProvider.class);
        when(provider.marketplace()).thenReturn(Collections.emptyList());
        final boolean result =
                Checker.confirmations(mock(ConfirmationProvider.class), "", SECRET, () -> provider);
        assertThat(result).isFalse();
    }

    private static ApiProvider mockApiThatReturnsOneLoan() {
        final RawLoan l = mock(RawLoan.class);
        final ApiProvider provider = mock(ApiProvider.class);
        doReturn(Collections.singletonList(l)).when(provider).marketplace();
        return provider;
    }

    @Test
    void confirmationsNotConfirming() {
        final ConfirmationProvider cp = mock(ConfirmationProvider.class);
        when(cp.requestConfirmation(any(), anyInt(),
                                    anyInt())).thenReturn(false);
        final boolean result = Checker.confirmations(cp, "", SECRET, CheckerTest::mockApiThatReturnsOneLoan);
        assertThat(result).isFalse();
    }

    @Test
    void confirmationsRejecting() {
        final ConfirmationProvider cp = mock(ConfirmationProvider.class);
        when(cp.requestConfirmation(any(), anyInt(),
                                    anyInt())).thenReturn(false);
        final boolean result = Checker.confirmations(cp, "", SECRET, CheckerTest::mockApiThatReturnsOneLoan);
        assertThat(result).isFalse();
    }

    @Test
    void confirmationsProper() {
        final ConfirmationProvider cp = mock(ConfirmationProvider.class);
        when(cp.requestConfirmation(any(), anyInt(),
                                    anyInt())).thenReturn(false);
        final boolean result = Checker.confirmations(cp, "", SECRET, CheckerTest::mockApiThatReturnsOneLoan);
        assertThat(result).isFalse();
    }

    @Test
    void notificationsEmptyOnInput() {
        assertThat(Checker.notifications("", Collections.emptyList())).isFalse();
    }

    @Test
    void notificationsEmptyByDefault() {
        assertThat(Checker.notifications("")).isFalse();
    }

    @Test
    void notificationsProper() {
        final EventListener<RoboZonkyTestingEvent> l = mock(EventListener.class);
        final EventListenerSupplier<RoboZonkyTestingEvent> r = () -> Optional.of(l);
        assertThat(Checker.notifications("", Collections.singletonList(r))).isTrue();
        verify(l).handle(any(RoboZonkyTestingEvent.class), any());
    }
}

