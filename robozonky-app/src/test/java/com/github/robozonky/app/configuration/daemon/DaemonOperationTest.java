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
import java.util.function.Consumer;

import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.portfolio.Portfolio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DaemonOperationTest extends AbstractZonkyLeveragingTest {

    @Test
    void exceptional() {
        final Tenant a = mock(Tenant.class);
        doThrow(IllegalStateException.class).when(a).run(any());
        final DaemonOperation d = new CustomOperation(a, null);
        d.run();
        assertThat(this.getNewEvents()).first().isInstanceOf(RoboZonkyDaemonFailedEvent.class);
    }

    @Test
    void standard() {
        final Tenant a = mock(Tenant.class);
        final BiConsumer<Portfolio, Tenant> operation = mock(BiConsumer.class);
        final DaemonOperation d = new CustomOperation(a, operation);
        d.run();
        verify(operation).accept(any(), eq(a));
        assertThat(d.getRefreshInterval()).isEqualByComparingTo(Duration.ofSeconds(1));
    }

    @Test
    void error() {
        final Tenant a = mock(Tenant.class);
        final BiConsumer<Portfolio, Tenant> operation = (p, api) -> {
            throw new Error();
        };
        final Consumer<Throwable> shutdown = mock(Consumer.class);
        final DaemonOperation d = new CustomOperation(a, operation, shutdown);
        d.run();
        verify(shutdown).accept(any());
    }

    private static final class CustomOperation extends DaemonOperation {

        private final BiConsumer<Portfolio, Tenant> operation;

        CustomOperation(final Tenant auth, final BiConsumer<Portfolio, Tenant> operation) {
            this(auth, operation, t -> {
            });
        }

        CustomOperation(final Tenant auth, final BiConsumer<Portfolio, Tenant> operation,
                        final Consumer<Throwable> shutdownHook) {
            super(shutdownHook, auth, () -> Optional.of(mock(Portfolio.class)), Duration.ofSeconds(1));
            this.operation = operation;
        }

        @Override
        protected boolean isEnabled(final Tenant authenticated) {
            return true;
        }

        @Override
        protected void execute(final Portfolio portfolio, final Tenant authenticated) {
            operation.accept(portfolio, authenticated);
        }
    }
}
