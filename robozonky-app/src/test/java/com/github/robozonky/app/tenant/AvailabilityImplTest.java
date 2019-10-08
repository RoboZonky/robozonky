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

package com.github.robozonky.app.tenant;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.RequestCounter;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.junit.jupiter.api.Test;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.Response;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.github.robozonky.app.tenant.AvailabilityImpl.MANDATORY_DELAY_IN_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvailabilityImplTest extends AbstractRoboZonkyTest {

    private final ZonkyApiTokenSupplier s = mock(ZonkyApiTokenSupplier.class);

    @Test
    void noAvailabilityOnClosedToken() {
        final Availability a = new AvailabilityImpl(s);
        when(s.isClosed()).thenReturn(true);
        assertSoftly(softly -> {
            softly.assertThat(a.nextAvailabilityCheck()).isEqualTo(Instant.MAX);
            softly.assertThat(a.isAvailable()).isFalse();
        });
    }

    @Test
    void nextAvailabilityWhileNotPaused() {
        final Availability a = new AvailabilityImpl(s);
        final Instant now = Instant.now();
        setClock(Clock.fixed(now, Defaults.ZONE_ID));
        assertSoftly(softly -> {
            softly.assertThat(a.nextAvailabilityCheck()).isEqualTo(now);
            softly.assertThat(a.isAvailable()).isTrue();
        });
    }

    @Test
    void scalingUnavailability() {
        final Availability a = new AvailabilityImpl(s);
        final Instant now = Instant.now();
        setClock(Clock.fixed(now, Defaults.ZONE_ID));
        final Response r = new ResponseBuilderImpl().build();
        final boolean reg = a.registerException(new ResponseProcessingException(r, UUID.randomUUID().toString()));
        assertSoftly(softly -> {
            softly.assertThat(reg).isTrue();
            softly.assertThat(a.isAvailable()).isFalse();
            softly.assertThat(a.nextAvailabilityCheck())
                    .isEqualTo(now.plus(Duration.ofSeconds(MANDATORY_DELAY_IN_SECONDS + 1)));
        });
        final boolean reg2 = a.registerException(new ClientErrorException(429));
        assertSoftly(softly -> {
            softly.assertThat(reg2).isFalse();
            softly.assertThat(a.isAvailable()).isFalse();
            softly.assertThat(a.nextAvailabilityCheck())
                    .isEqualTo(now.plus(Duration.ofSeconds(MANDATORY_DELAY_IN_SECONDS + 2)));
        });
        final boolean reg3 = a.registerException(new ServerErrorException(503));
        assertSoftly(softly -> {
            softly.assertThat(reg3).isFalse();
            softly.assertThat(a.isAvailable()).isFalse();
            softly.assertThat(a.nextAvailabilityCheck())
                    .isEqualTo(now.plus(Duration.ofSeconds(MANDATORY_DELAY_IN_SECONDS + 4)));
        });
        final Optional<Instant> success = a.registerSuccess();
        assertSoftly(softly -> {
            softly.assertThat(success).isPresent();
            softly.assertThat(a.isAvailable()).isTrue();
            softly.assertThat(a.nextAvailabilityCheck()).isEqualTo(now);
        });
    }

    @Test
    void noSuccessUntilRequestCounterIncrease() {
        final RequestCounter counter = mock(RequestCounter.class);
        final Availability a = new AvailabilityImpl(s, counter);
        final Instant now = Instant.now();
        setClock(Clock.fixed(now, Defaults.ZONE_ID));
        a.registerException(new IllegalStateException());
        assertThat(a.isAvailable()).isFalse();
        // move time, but don't increase the request counter
        setClock(Clock.fixed(now.plus(Duration.ofMinutes(1)), Defaults.ZONE_ID));
        assertThat(a.registerSuccess()).isEmpty();
        assertThat(a.isAvailable()).isFalse();
        // move time and increase the request counter
        setClock(Clock.fixed(now.plus(Duration.ofMinutes(2)), Defaults.ZONE_ID));
        when(counter.current()).thenReturn(2L);
        assertThat(a.registerSuccess()).contains(now);
        assertThat(a.isAvailable()).isTrue();
    }

    @Test
    void longerDelayWhenHttp429Encountered() {
        final Availability a = new AvailabilityImpl(s);
        final Instant now = Instant.now();
        setClock(Clock.fixed(now, Defaults.ZONE_ID));
        final Exception ex = new ClientErrorException(Response.Status.TOO_MANY_REQUESTS);
        final boolean reg = a.registerException(ex);
        assertSoftly(softly -> {
            softly.assertThat(reg).isTrue();
            softly.assertThat(a.isAvailable()).isFalse();
            softly.assertThat(a.nextAvailabilityCheck())
                    .isEqualTo(now.plus(Duration.ofSeconds(60 + 1)));
        });
    }

}
