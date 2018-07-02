/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.notifications.listeners;

import java.math.BigDecimal;
import java.util.Collections;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;
import com.github.robozonky.notifications.Target;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BalanceOnTargetEventListenerTest extends AbstractRoboZonkyTest {

    private static final SessionInfo SESSION_INFO = new SessionInfo("someone@somewhere.cz");

    @Test
    void check() throws Exception {
        final AbstractTargetHandler h = mock(AbstractTargetHandler.class);
        doNothing().when(h).offer(any());
        when(h.getTarget()).thenReturn(Target.EMAIL);
        when(h.getListenerSpecificIntProperty(eq(SupportedListener.BALANCE_ON_TARGET), eq("targetBalance"), anyInt()))
                .thenAnswer(i -> i.getArgument(2));
        final PortfolioOverview p = PortfolioOverview.calculate(BigDecimal.valueOf(1000), Collections.emptyMap());
        final ExecutionStartedEvent evt = new ExecutionStartedEvent(Collections.emptyList(), p);
        final AbstractListener<ExecutionStartedEvent> l =
                new BalanceOnTargetEventListener(SupportedListener.BALANCE_ON_TARGET, h);
        l.handle(evt, SESSION_INFO); // balance change
        verify(h, times(1)).offer(any());
        l.handle(evt, SESSION_INFO); // no change, no notification
        verify(h, times(1)).offer(any());
        final PortfolioOverview p2 = PortfolioOverview.calculate(BigDecimal.ZERO, Collections.emptyMap());
        final ExecutionStartedEvent evt2 = new ExecutionStartedEvent(Collections.emptyList(), p2);
        l.handle(evt2, SESSION_INFO); // no change, no notification
        verify(h, times(1)).offer(any());
        l.handle(evt, SESSION_INFO); // back over threshold, send notification
        verify(h, times(2)).offer(any());
    }
}
