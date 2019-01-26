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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.api.remote.entities.RawLoan;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.state.TenantState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CheckerTest {

    private static final char[] SECRET = new char[0];
    private static final SessionInfo SESSION_INFO = new SessionInfo(UUID.randomUUID().toString());
    @Mock
    private EventListener<RoboZonkyTestingEvent> l;

    private static ApiProvider mockApiThatReturnsOneLoan() {
        final RawLoan l = Mockito.mock(RawLoan.class);
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        Mockito.doReturn(Collections.singletonList(l)).when(provider).marketplace();
        return provider;
    }

    @Test
    void confirmationsMarketplaceFail() {
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        Mockito.doThrow(new IllegalStateException("Testing")).when(provider).marketplace();
        final boolean result =
                Checker.confirmations(Mockito.mock(ConfirmationProvider.class), "", SECRET, () -> provider);
        assertThat(result).isFalse();
    }

    @Test
    void confirmationsMarketplaceWithoutLoans() {
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        Mockito.when(provider.marketplace()).thenReturn(Collections.emptyList());
        final boolean result =
                Checker.confirmations(Mockito.mock(ConfirmationProvider.class), "", SECRET, () -> provider);
        assertThat(result).isFalse();
    }

    @Test
    void confirmationsNotConfirming() {
        final ConfirmationProvider cp = Mockito.mock(ConfirmationProvider.class);
        Mockito.when(cp.requestConfirmation(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                            ArgumentMatchers.anyInt())).thenReturn(false);
        final boolean result = Checker.confirmations(cp, "", SECRET, CheckerTest::mockApiThatReturnsOneLoan);
        assertThat(result).isFalse();
    }

    @Test
    void confirmationsRejecting() {
        final ConfirmationProvider cp = Mockito.mock(ConfirmationProvider.class);
        Mockito.when(cp.requestConfirmation(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                            ArgumentMatchers.anyInt())).thenReturn(false);
        final boolean result = Checker.confirmations(cp, "", SECRET, CheckerTest::mockApiThatReturnsOneLoan);
        assertThat(result).isFalse();
    }

    @Test
    void confirmationsProper() {
        final ConfirmationProvider cp = Mockito.mock(ConfirmationProvider.class);
        Mockito.when(cp.requestConfirmation(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                            ArgumentMatchers.anyInt())).thenReturn(false);
        final boolean result = Checker.confirmations(cp, "", SECRET, CheckerTest::mockApiThatReturnsOneLoan);
        assertThat(result).isFalse();
    }

    @Test
    void notificationsEmptyOnInput() {
        assertThat(Checker.notifications(SESSION_INFO, Collections.emptyList())).isFalse();
    }

    @Test
    void notificationsEmptyByDefault() throws MalformedURLException {
        assertThat(Checker.notifications(SESSION_INFO, new URL("file:///something"))).isFalse();
    }

    @BeforeEach
    @AfterEach
    void destroyState() {
        TenantState.destroyAll();
    }

    @Test
    void notificationsProper() {
        final EventListenerSupplier<RoboZonkyTestingEvent> r = () -> Optional.of(l);
        assertThat(Checker.notifications(SESSION_INFO, Collections.singletonList(r))).isTrue();
        Mockito.verify(l).handle(ArgumentMatchers.any(RoboZonkyTestingEvent.class), ArgumentMatchers.any());
    }
}

