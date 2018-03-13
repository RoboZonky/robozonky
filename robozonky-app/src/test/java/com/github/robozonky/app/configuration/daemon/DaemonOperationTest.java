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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DaemonOperationTest extends AbstractZonkyLeveragingTest {

    @Test
    void exceptional() {
        final Authenticated a = mock(Authenticated.class);
        doThrow(IllegalStateException.class).when(a).run(any());
        final DaemonOperation d = new CustomOperation(a, null);
        d.run();
        assertThat(this.getNewEvents()).first().isInstanceOf(RoboZonkyDaemonFailedEvent.class);
    }

    @Test
    void standard() {
        final Authenticated a = mock(Authenticated.class);
        final BiConsumer<Portfolio, Authenticated> operation = mock(BiConsumer.class);
        final DaemonOperation d = new CustomOperation(a, operation);
        d.run();
        verify(operation).accept(any(), eq(a));
        assertThat(d.getRefreshInterval()).isEqualByComparingTo(Duration.ofSeconds(1));
    }

    private static final class CustomOperation extends DaemonOperation {

        private final BiConsumer<Portfolio, Authenticated> operation;

        CustomOperation(final Authenticated auth, final BiConsumer<Portfolio, Authenticated> operation) {
            super((t) -> {
            }, auth, () -> Optional.of(mock(Portfolio.class)), Duration.ofSeconds(1));
            this.operation = operation;
        }

        @Override
        protected boolean isEnabled(final Authenticated authenticated) {
            return true;
        }

        @Override
        protected void execute(Portfolio portfolio, Authenticated authenticated) {
            operation.accept(portfolio, authenticated);
        }
    }
}
