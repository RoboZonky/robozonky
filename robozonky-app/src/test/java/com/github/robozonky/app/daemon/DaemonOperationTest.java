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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.Tenant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class DaemonOperationTest extends AbstractZonkyLeveragingTest {

    @Mock
    private BiConsumer<Portfolio, Tenant> operation;
    @Mock
    private Consumer<Throwable> shutdown;

    @Test
    void exceptional() {
        final Tenant a = mockTenant();
        doThrow(IllegalStateException.class).when(a).run(any());
        final DaemonOperation d = new CustomOperation(a, null);
        d.run();
        assertThat(getEventsRequested()).first().isInstanceOf(RoboZonkyDaemonFailedEvent.class);
    }

    @Test
    void standard() {
        final Tenant a = mockTenant();
        final DaemonOperation d = new CustomOperation(a, operation);
        d.run();
        verify(operation).accept(any(), eq(a));
        assertThat(d.getRefreshInterval()).isEqualByComparingTo(Duration.ofSeconds(1));
    }

    @Test
    void error() {
        final Tenant a = mockTenant();
        final BiConsumer<Portfolio, Tenant> operation = (p, api) -> {
            throw new Error();
        };
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
