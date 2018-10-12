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

package com.github.robozonky.app.management;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.events.EventFactory;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.mock;

class JmxListenerServiceTest extends AbstractRoboZonkyTest {

    private static final String USERNAME = "someone@somewhere.cz";
    private static Optional<Consumer<ShutdownHook.Result>> MGMT;

    private static com.github.robozonky.app.management.Runtime getRuntimeMBean() {
        return (com.github.robozonky.app.management.Runtime) JmxListenerService.getMBean(MBean.RUNTIME)
                .orElseThrow(() -> new IllegalStateException("Not found"));
    }

    @BeforeAll
    static void loadAll() {
        MGMT = new Management(mock(Lifecycle.class)).get();
    }

    @AfterAll
    static void unloadAll() {
        MGMT.ifPresent(rc -> rc.accept(new ShutdownHook.Result(ReturnCode.OK, null)));
    }

    private DynamicTest getParametersForExecutionCompleted() {
        final ExecutionCompletedEvent evt = EventFactory.executionCompleted(Collections.emptyList(), null);
        final Consumer<SoftAssertions> before = (softly) -> {
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(getRuntimeMBean().getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return getParameters(evt, before, after);
    }

    private DynamicTest getParameters(final Event evt, final Consumer<SoftAssertions> before,
                                      final Consumer<SoftAssertions> after) {
        return dynamicTest(evt.getClass().getSimpleName(), () -> testSet(evt, before, after));
    }

    private void testSet(final Event event, final Consumer<SoftAssertions> assertionsBefore,
                         final Consumer<SoftAssertions> assertionsAfter) {
        assertSoftly(assertionsBefore);
        handleEvent(event);
        assertSoftly(assertionsAfter);
    }

    @TestFactory
    Stream<DynamicTest> events() {
        return Stream.of(getParametersForExecutionCompleted());
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void handleEvent(final T event) {
        final JmxListenerService service = new JmxListenerService();
        final EventListenerSupplier<T> r = service.findListeners((Class<T>) event.getClass())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No event listener found."));
        final EventListener<T> listener = r.get().get();
        listener.handle(event, new SessionInfo(USERNAME));
    }

    @Test
    void setInvalid() {
        final JmxListenerService service = new JmxListenerService();
        final Optional<EventListenerSupplier<RoboZonkyTestingEvent>> r =
                service.findListeners(RoboZonkyTestingEvent.class).findFirst();
        assertThat(r).isEmpty();
    }
}
