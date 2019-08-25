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

package com.github.robozonky.app.daemon;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.ResponseProcessingException;

import com.github.robozonky.api.notifications.RoboZonkyDaemonSuspendedEvent;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.app.tenant.TenantBuilder;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.secrets.SecretProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SkippableTest extends AbstractZonkyLeveragingTest {

    @Test
    void fails() {
        final Runnable r = mock(Runnable.class);
        doThrow(IllegalStateException.class).when(r).run();
        final PowerTenant t = mockTenant();
        final Skippable s = new Skippable(r, t);
        s.run();
        assertThat(this.getEventsRequested()).hasSize(1)
                .first()
                .isInstanceOf(RoboZonkyDaemonSuspendedEvent.class);
    }

    @Test
    void dies() {
        final Runnable r = mock(Runnable.class);
        doThrow(OutOfMemoryError.class).when(r).run();
        final PowerTenant t = mockTenant();
        final Consumer<Throwable> c = mock(Consumer.class);
        final Skippable s = new Skippable(r, t, c);
        assertThatThrownBy(s::run).isInstanceOf(OutOfMemoryError.class);
        verify(c, only()).accept(any());
    }

    @Test
    void unavailable() {
        final Instant now = Instant.now();
        setClock(Clock.fixed(now, Defaults.ZONE_ID));
        final Runnable r = mock(Runnable.class);
        doThrow(ClientErrorException.class).when(r).run();
        final PowerTenant t = new TenantBuilder()
                .withApi(new ApiProvider(null))
                .withSecrets(SecretProvider.inMemory("someone@somewhere.cz"))
                .build();
        final Skippable s = new Skippable(r, t);
        logger.debug("First run.");
        s.run();
        verify(r, times(1)).run();
        assertThat(t.getAvailability().isAvailable()).isFalse();
        // move one second, make sure it checks again
        final int mandatoryDelay = 5;
        setClock(Clock.fixed(now.plus(Duration.ofSeconds(mandatoryDelay + 1)), Defaults.ZONE_ID));
        logger.debug("Second run.");
        doThrow(ServerErrorException.class).when(r).run();
        s.run();
        verify(r, times(2)).run();
        assertThat(t.getAvailability().isAvailable()).isFalse();
        // but it failed again, exponential backoff in effect
        setClock(Clock.fixed(now.plus(Duration.ofSeconds(mandatoryDelay + 2)), Defaults.ZONE_ID));
        logger.debug("Third run.");
        doThrow(ResponseProcessingException.class).when(r).run();
        s.run();
        verify(r, times(3)).run();
        assertThat(t.getAvailability().isAvailable()).isFalse();
        setClock(Clock.fixed(now.plus(Duration.ofSeconds(mandatoryDelay + 3)), Defaults.ZONE_ID));
        logger.debug("Fourth run.");
        doNothing().when(r).run();
        s.run();
        verify(r, times(3)).run(); // not run as we're in the exponential backoff
        assertThat(t.getAvailability().isAvailable()).isFalse();
        setClock(Clock.fixed(now.plus(Duration.ofSeconds(mandatoryDelay + 4)), Defaults.ZONE_ID));
        logger.debug("Fourth run.");
        s.run();
        verify(r, times(4)).run(); // it was run now
        assertThat(t.getAvailability().isAvailable()).isTrue();
    }

}

