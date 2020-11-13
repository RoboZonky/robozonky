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

package com.github.robozonky.cli;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.internal.SessionInfoImpl;
import com.github.robozonky.internal.extensions.ListenerServiceLoader;
import com.github.robozonky.internal.test.DateUtil;

@Command(name = "notification-tester", description = NotificationTestingFeature.DESCRIPTION)
public final class NotificationTestingFeature extends AbstractFeature {

    static final String DESCRIPTION = "Send a testing notification using the provided configuration.";

    @Option(names = { "-u", "--username" }, description = "Zonky username.", required = true)
    private String username = null;
    @Option(names = { "-l", "--location" }, description = "URL leading to the configuration.", required = true)
    private URL location;

    public NotificationTestingFeature(final String username, final URL location) {
        this.username = username;
        this.location = location;
    }

    NotificationTestingFeature() {
        // for Picocli
    }

    static boolean notifications(final SessionInfo sessionInfo, final URL configurationLocation) {
        ListenerServiceLoader.registerConfiguration(sessionInfo, configurationLocation);
        return notifications(sessionInfo, ListenerServiceLoader.load(sessionInfo, RoboZonkyTestingEvent.class));
    }

    static boolean notifications(final SessionInfo sessionInfo,
            final List<EventListenerSupplier<RoboZonkyTestingEvent>> refreshables) {
        final Collection<EventListener<RoboZonkyTestingEvent>> listeners = refreshables.stream()
            .flatMap(r -> r.get()
                .stream())
            .collect(Collectors.toSet());
        if (listeners.isEmpty()) {
            return false;
        } else {
            final RoboZonkyTestingEvent evt = DateUtil::zonedNow;
            listeners.forEach(l -> l.handle(evt, sessionInfo));
            return true;
        }
    }

    @Override
    public String describe() {
        return DESCRIPTION;
    }

    @Override
    public void setup() {
        // nothing to do
    }

    @Override
    public void test() throws TestFailedException {
        final boolean success = notifications(new SessionInfoImpl(username), location);
        if (!success) {
            throw new TestFailedException("No notifications have been sent. Check log for possible problems.");
        }
        logger.info("Notifications should have been sent now.");
    }
}
