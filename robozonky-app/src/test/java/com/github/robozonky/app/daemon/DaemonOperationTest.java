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

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class DaemonOperationTest extends AbstractZonkyLeveragingTest {

    @Mock
    private Consumer<Tenant> operation;

    @Test
    void exceptional() {
        final PowerTenant a = mockTenant();
        final DaemonOperation d = new CustomOperation(a, null);
        assertThatThrownBy(d::run).isInstanceOf(NullPointerException.class);
    }

    @Test
    void standard() {
        final PowerTenant a = mockTenant();
        final DaemonOperation d = new CustomOperation(a, operation);
        d.run();
        verify(operation).accept(eq(a));
        assertThat(d.getRefreshInterval()).isEqualByComparingTo(Duration.ofSeconds(1));
    }

    private static final class CustomOperation extends DaemonOperation {

        private final Consumer<Tenant> operation;

        CustomOperation(final PowerTenant auth, final Consumer<Tenant> operation) {
            super(auth, Duration.ofSeconds(1));
            this.operation = operation;
        }

        @Override
        protected boolean isEnabled(final Tenant tenant) {
            return true;
        }

        @Override
        protected boolean hasStrategy(final Tenant tenant) {
            return true;
        }

        @Override
        protected void execute(final Tenant tenant) {
            operation.accept(tenant);
        }
    }
}
