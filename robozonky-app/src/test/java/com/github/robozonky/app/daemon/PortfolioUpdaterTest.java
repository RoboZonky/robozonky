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
import java.util.Optional;
import java.util.function.Consumer;

import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static com.github.robozonky.app.events.EventFactory.roboZonkyTesting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class PortfolioUpdaterTest extends AbstractZonkyLeveragingTest {

    @Mock
    private Consumer<Throwable> t;

    @Test
    void creation() {
        final PortfolioUpdater instance = PortfolioUpdater.create(t, mockTenant(), Optional::empty);
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
        final PortfolioUpdater instance = new PortfolioUpdater(t, a, BlockedAmountProcessor.createLazy(a),
                                                               Duration.ofSeconds(2));
        instance.registerDependant(tp -> { // fire event
            tp.fire(roboZonkyTesting());
        });
        instance.registerDependant(tp -> { // fail
            throw new IllegalStateException("Testing exception");
        });
        instance.run();
        assertSoftly(softly -> {
            softly.assertThat(instance.get()).isEmpty();
            softly.assertThat(instance.isInitializing()).isTrue();
            softly.assertThat(getEventsRequested()).isEmpty();
        });
        verify(t).accept(notNull());
    }
}
