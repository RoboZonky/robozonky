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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.Response;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.assertj.core.api.SoftAssertions;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AvailabilityImplTest extends AbstractRoboZonkyTest {

    private final ZonkyApiTokenSupplier s = mock(ZonkyApiTokenSupplier.class);
    private final Availability a = new AvailabilityImpl(s);

    @Test
    void noAvailabilityOnClosedToken() {
        when(s.isClosed()).thenReturn(true);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(a.nextAvailabilityCheck()).isEqualTo(Instant.MAX);
            softly.assertThat(a.isAvailable()).isFalse();
        });
    }

    @Test
    void nextAvailabilityWhileNotPaused() {
        final Instant now = Instant.now();
        setClock(Clock.fixed(now, Defaults.ZONE_ID));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(a.nextAvailabilityCheck()).isEqualTo(now);
            softly.assertThat(a.isAvailable()).isTrue();
        });
    }

    @Test
    void scalingUnavailability() {
        final Instant now = Instant.now();
        setClock(Clock.fixed(now, Defaults.ZONE_ID));
        final Response r = new ResponseBuilderImpl().build();
        a.registerApiIssue(new ResponseProcessingException(r, UUID.randomUUID().toString()));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(a.isAvailable()).isFalse();
            softly.assertThat(a.nextAvailabilityCheck()).isEqualTo(now.plus(Duration.ofSeconds(1)));
        });
        a.registerClientError(new ClientErrorException(429));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(a.isAvailable()).isFalse();
            softly.assertThat(a.nextAvailabilityCheck()).isEqualTo(now.plus(Duration.ofSeconds(2)));
        });
        a.registerServerError(new ServerErrorException(503));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(a.isAvailable()).isFalse();
            softly.assertThat(a.nextAvailabilityCheck()).isEqualTo(now.plus(Duration.ofSeconds(4)));
        });
        a.registerAvailability();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(a.isAvailable()).isTrue();
            softly.assertThat(a.nextAvailabilityCheck()).isEqualTo(now);
        });
    }

}
