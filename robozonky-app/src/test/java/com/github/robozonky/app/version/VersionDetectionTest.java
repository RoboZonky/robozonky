/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.app.version;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.RoboZonkyExperimentalUpdateDetectedEvent;
import com.github.robozonky.api.notifications.RoboZonkyUpdateDetectedEvent;
import com.github.robozonky.app.events.AbstractEventLeveragingTest;
import com.github.robozonky.internal.jobs.SimplePayload;
import com.github.robozonky.internal.util.functional.Either;

class VersionDetectionTest extends AbstractEventLeveragingTest {

    private final Supplier<Either<Throwable, Response>> metadata = mock(Supplier.class);
    private final SimplePayload payload = new VersionDetection(metadata);

    @Test
    void live() {
        final SimplePayload livePayload = new VersionDetection();
        livePayload.run();
        assertThat(getEventsRequested()).isEmpty();
    }

    @Test
    void noEventsOnFailure() {
        when(metadata.get()).thenReturn(Either.left(new IllegalStateException()));
        payload.run();
        assertThat(getEventsRequested()).isEmpty();
    }

    @Test
    void noNewerVersions() {
        when(metadata.get()).thenReturn(Either.right(Response.noMoreRecentVersion()));
        payload.run();
        assertThat(getEventsRequested()).isEmpty();
    }

    @Nested
    class FirstCheck {

        @BeforeEach
        void before() {
            when(metadata.get())
                .thenReturn(
                        Either.right(Response.moreRecent(new GithubRelease("5.0.1"), new GithubRelease("5.0.2-cr-1"))));
        }

        @Test
        void triggerEvents() {
            payload.run();
            final List<Event> events = getEventsRequested();
            assertThat(events)
                .hasSize(2)
                .first()
                .isInstanceOf(RoboZonkyUpdateDetectedEvent.class);
            assertThat(events)
                .last()
                .isInstanceOf(RoboZonkyExperimentalUpdateDetectedEvent.class);
        }

        @Nested
        class SecondCheckStableOnly {

            @BeforeEach
            void before() {
                when(metadata.get())
                    .thenReturn(Either.right(Response.moreRecentStable(new GithubRelease("5.0.1"))));
            }

            @Test
            void triggerEvents() {
                payload.run();
                assertThat(getEventsRequested())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(RoboZonkyUpdateDetectedEvent.class);
            }

        }

        @Nested
        class SecondCheckExperimentalOnly {

            @BeforeEach
            void before() {
                when(metadata.get())
                    .thenReturn(Either.right(Response.moreRecentExperimental(new GithubRelease("5.0.2-cr-1"))));
            }

            @Test
            void triggerEvents() {
                payload.run();
                assertThat(getEventsRequested())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(RoboZonkyExperimentalUpdateDetectedEvent.class);
            }

        }
    }

}
