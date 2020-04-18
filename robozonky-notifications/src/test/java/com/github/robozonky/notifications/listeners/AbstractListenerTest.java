/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.notifications.listeners;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.extensions.ListenerServiceLoader;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.ConfigStorage;
import com.github.robozonky.notifications.NotificationListenerService;
import com.github.robozonky.notifications.SupportedListener;
import com.github.robozonky.notifications.Target;
import com.github.robozonky.notifications.templates.TemplateProcessor;
import com.github.robozonky.test.AbstractRoboZonkyTest;

import freemarker.template.TemplateException;

public class AbstractListenerTest extends AbstractRoboZonkyTest {

    private static final SessionInfo SESSION = mockSessionInfo();
    private static final RoboZonkyTestingEvent EVENT = OffsetDateTime::now;

    private static AbstractListener<? extends Event> getListener(final SupportedListener s,
            final AbstractTargetHandler p) {
        final AbstractListener<? extends Event> e = spy((AbstractListener<? extends Event>) s.getListener(p));
        // always return a listener that WILL send an e-mail, even though this means shouldSendEmail() is not tested
        doReturn(true).when(e)
            .shouldNotify(any(), eq(SESSION));
        return e;
    }

    private static <T extends Event> void testFormal(final AbstractListener<T> listener, final T event,
            final SupportedListener listenerType) {
        assertThat(event).isInstanceOf(listenerType.getSampleEvent()
            .getClass());
        assertThat(listener.getSubject(event)).isNotEmpty();
        assertThat(listener.getTemplateFileName())
            .isNotNull()
            .isNotEmpty();
    }

    private static void testListenerEnabled(final Event event) {
        final NotificationListenerService service = new NotificationListenerService();
        final Stream<? extends EventListenerSupplier<? extends Event>> supplier = service.findListeners(SESSION,
                event.getClass());
        assertThat(supplier).isNotEmpty();
    }

    private static AbstractTargetHandler getHandler(final ConfigStorage storage) {
        return spy(new TestingTargetHandler(storage));
    }

    static AbstractTargetHandler getHandler() {
        try {
            final ConfigStorage cs = ConfigStorage
                .create(AbstractListenerTest.class.getResourceAsStream("notifications-enabled.cfg"));
            return getHandler(cs);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T extends Event> void testTriggered(final AbstractTargetHandler h,
            final AbstractListener<T> listener,
            final T event) throws Exception {
        listener.handle(event, SESSION);
        verify(h, times(1)).send(eq(SESSION), notNull(), notNull(), notNull());
    }

    private <T extends Event> void testPlainTextProcessing(final AbstractListener<T> listener, final T event)
            throws IOException, TemplateException {
        final String s = TemplateProcessor.INSTANCE.processPlainText(listener.getTemplateFileName(),
                listener.getData(event, SESSION));
        logger.debug("Plain text was: {}.", s);
        assertThat(s).contains(Defaults.ROBOZONKY_URL);
        assertThat(s).contains("uživatel"); // check that UTF-8 is properly encoded
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> DynamicContainer forListener(final SupportedListener listener) {
        final AbstractTargetHandler p = getHandler();
        final AbstractListener<T> l = (AbstractListener<T>) getListener(listener, p);
        final T e = (T) listener.getSampleEvent();
        return DynamicContainer.dynamicContainer(listener.toString(), Stream.of(
                dynamicTest("is formally correct", () -> testFormal(l, e, listener)),
                dynamicTest("is processed as plain text", () -> testPlainTextProcessing(l, e)),
                dynamicTest("is processed as HTML", () -> testHtmlProcessing(l, e)),
                dynamicTest("triggers the sending code", () -> testTriggered(p, l, e)),
                dynamicTest("has listener enabled", () -> testListenerEnabled(e))));
    }

    private <T extends Event> void testHtmlProcessing(final AbstractListener<T> listener,
            final T event) throws IOException, TemplateException {
        final Map<String, Object> data = new HashMap<>(listener.getData(event, SESSION));
        data.put("subject", UUID.randomUUID()
            .toString());
        final String s = TemplateProcessor.INSTANCE.processHtml(listener.getTemplateFileName(),
                Collections.unmodifiableMap(data));
        logger.debug("HTML text was: {}.", s);
        assertThat(s).contains(Defaults.ROBOZONKY_URL);
        assertThat(s).contains("uživatel"); // check that UTF-8 is properly encoded
    }

    @BeforeEach
    void configureNotifications() throws URISyntaxException, MalformedURLException {
        ListenerServiceLoader.registerConfiguration(SESSION,
                AbstractListenerTest.class.getResource(
                        "notifications-enabled.cfg")
                    .toURI()
                    .toURL());
    }

    @Test
    void spamProtectionAvailable() throws Exception {
        final ConfigStorage cs = ConfigStorage
            .create(getClass().getResourceAsStream("notifications-enabled-spamless.cfg"));
        final AbstractTargetHandler p = getHandler(cs);
        final TestingEmailingListener l = new TestingEmailingListener(p);
        l.handle(EVENT, SESSION);
        verify(p, times(1)).send(notNull(), notNull(), notNull(), notNull());
        l.handle(EVENT, SESSION);
        // e-mail not re-sent, finisher not called again
        verify(p, times(1)).send(notNull(), notNull(), notNull(), notNull());
    }

    @BeforeEach
    @AfterEach
    @Override
    protected void deleteState() { // JUnit 5 won't invoke this from an abstract class
        super.deleteState();
    }

    @TestFactory
    Stream<DynamicNode> listeners() {
        // create events for listeners
        return Stream.of(SupportedListener.values())
            .map(this::forListener);
    }

    private static class TestingTargetHandler extends AbstractTargetHandler {

        public TestingTargetHandler(final ConfigStorage storage) {
            super(storage, Target.EMAIL);
        }

        @Override
        public void send(final SessionInfo sessionInfo, final String subject, final String message,
                final String fallbackMessage) {

        }
    }

    private static final class TestingEmailingListener extends AbstractListener<RoboZonkyTestingEvent> {

        TestingEmailingListener(final AbstractTargetHandler handler) {
            super(SupportedListener.TESTING, handler);
        }

        @Override
        public String getSubject(final RoboZonkyTestingEvent event) {
            return "No actual subject";
        }

        @Override
        public String getTemplateFileName() {
            return "testing.ftl";
        }
    }

}
