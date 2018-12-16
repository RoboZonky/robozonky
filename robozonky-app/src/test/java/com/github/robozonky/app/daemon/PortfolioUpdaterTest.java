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

package com.github.robozonky.app.daemon;

import java.time.Duration;
import java.util.function.Consumer;

import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyTesting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PortfolioUpdaterTest extends AbstractZonkyLeveragingTest {

    @Test
    void creation() {
        final Consumer<Throwable> t = mock(Consumer.class);
        final PortfolioUpdater instance = PortfolioUpdater.create(t, mockTenant());
        assertSoftly(softly -> {
            softly.assertThat(instance.isInitializing()).isTrue(); // by default it's true
            softly.assertThat(instance.getRegisteredDependants()).hasSize(4); // expected contents
        });
    }

    @Test
    void updatingDependants() {
        final Zonky z = harmlessZonky(10_000);
        final Tenant a = mockTenant(z);
        final PortfolioDependant dependant = tp -> tp.fire(roboZonkyTesting());
        final PortfolioUpdater instance = new PortfolioUpdater(a, BlockedAmountProcessor.createLazy(a));
        instance.registerDependant(dependant);
        instance.run();
        // make sure that the dependants were called with the proper value of Portfolio
        assertThat(getEventsRequested()).first().isInstanceOf(RoboZonkyTestingEvent.class);
        assertThat(instance.isInitializing()).isFalse(); // it's false when update finished
    }

    @Test
    void backoffFailed() {
        final Zonky z = harmlessZonky(10_000);
        final Tenant a = mockTenant(z);
        final Consumer<Throwable> t = mock(Consumer.class);
        final PortfolioUpdater instance = new PortfolioUpdater(t, a, BlockedAmountProcessor.createLazy(a),
                                                               Duration.ofSeconds(2));
        final PortfolioDependant d = mock(PortfolioDependant.class);
        instance.registerDependant(tp -> { // fail
            throw new IllegalStateException("Testing exception");
        });
        instance.registerDependant(d);
        Assertions.assertTimeout(Duration.ofSeconds(10), instance::run);
        assertThat(instance.isInitializing()).isTrue();
        verify(d, never()).accept(any());
        verify(t).accept(notNull());
    }
}
