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
import java.util.function.BiConsumer;

import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class DaemonOperationTest extends AbstractZonkyLeveragingTest {

    private static final class CustomOperation extends DaemonOperation {

        private final BiConsumer<Portfolio, Authenticated> operation;

        public CustomOperation(final Authenticated auth, final BiConsumer<Portfolio, Authenticated> operation) {
            super(auth, () -> Optional.of(Mockito.mock(Portfolio.class)), Duration.ofSeconds(1));
            this.operation = operation;
        }

        @Override
        protected boolean isEnabled(final Authenticated authenticated) {
            return true;
        }

        @Override
        protected BiConsumer<Portfolio, Authenticated> getInvestor() {
            return operation;
        }
    }

    @Test
    public void exceptional() {
        final Authenticated a = Mockito.mock(Authenticated.class);
        Mockito.doThrow(IllegalStateException.class).when(a).run(ArgumentMatchers.any());
        final DaemonOperation d = new CustomOperation(a, null);
        d.run();
        Assertions.assertThat(this.getNewEvents()).first().isInstanceOf(RoboZonkyDaemonFailedEvent.class);
    }

    @Test
    public void standard() {
        final Authenticated a = Mockito.mock(Authenticated.class);
        final BiConsumer<Portfolio, Authenticated> operation = Mockito.mock(BiConsumer.class);
        final DaemonOperation d = new CustomOperation(a, operation);
        d.run();
        Mockito.verify(operation).accept(ArgumentMatchers.any(), ArgumentMatchers.eq(a));
        Assertions.assertThat(d.getRefreshInterval()).isEqualByComparingTo(Duration.ofSeconds(1));
    }
}
