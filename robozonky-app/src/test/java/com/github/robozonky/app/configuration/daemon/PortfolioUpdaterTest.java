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

package com.github.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.portfolio.PortfolioDependant;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class PortfolioUpdaterTest extends AbstractZonkyLeveragingTest {

    @Test
    void creation() {
        final PortfolioUpdater instance = PortfolioUpdater.create((t) -> {
                                                                  }, mock(Authenticated.class), Optional::empty,
                                                                  true);
        assertSoftly(softly -> {
            softly.assertThat(instance.isUpdating()).isTrue(); // by default it's true
            softly.assertThat(instance.getRegisteredDependants()).hasSize(4); // expected contents
        });
    }

    @Test
    void updatingDependants() {
        final Zonky z = harmlessZonky(10_000);
        final Authenticated a = mockAuthentication(z);
        final PortfolioDependant dependant = mock(PortfolioDependant.class);
        final PortfolioUpdater instance = new PortfolioUpdater((t) -> {
        }, a, mockBalance(z));
        instance.registerDependant(dependant);
        instance.run();
        verify(a, atLeast(2)).call(any());
        final Optional<Portfolio> result = instance.get();
        // make sure that the dependants were called with the proper value of Portfolio
        verify(dependant).accept(eq(result.get()), eq(a));
        assertThat(instance.isUpdating()).isFalse(); // it's false when update finished
    }

    @Test
    void backoffFailed() {
        final Zonky z = harmlessZonky(10_000);
        doThrow(IllegalStateException.class).when(z).getInvestments((Select) any()); // will always fail
        final Authenticated a = mockAuthentication(z);
        final Consumer<Throwable> t = mock(Consumer.class);
        final PortfolioUpdater instance = new PortfolioUpdater(t, a, mockBalance(z), Duration.ofSeconds(2));
        instance.run();
        assertSoftly(softly -> {
            softly.assertThat(instance.get()).isEmpty();
            softly.assertThat(instance.isUpdating()).isTrue();
        });
        verify(t).accept(notNull());
    }
}
