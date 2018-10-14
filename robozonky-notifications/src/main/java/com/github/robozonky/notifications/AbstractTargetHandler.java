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

package com.github.robozonky.notifications;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;

import com.github.robozonky.api.SessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTargetHandler {

    private static final String HOURLY_LIMIT = "hourlyMaxEmails";
    protected final Target target;
    final ConfigStorage config;
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final Counter notifications;
    private final Map<SupportedListener, Counter> specificNotifications = new EnumMap<>(SupportedListener.class);

    protected AbstractTargetHandler(final ConfigStorage config, final Target target) {
        this.config = config;
        this.target = target;
        this.notifications = new Counter("global", getHourlyLimit(), Duration.ofHours(1));
    }

    private static String getCompositePropertyName(final SupportedListener listener, final String property) {
        return listener.getLabel() + "." + property;
    }

    public Target getTarget() {
        return target;
    }

    private int getHourlyLimit(final SupportedListener listener) {
        return this.getListenerSpecificIntProperty(listener, HOURLY_LIMIT, Integer.MAX_VALUE);
    }

    public int getListenerSpecificIntProperty(final SupportedListener listener, final String property,
                                              final int defaultValue) {
        return this.getListenerSpecificIntProperty(listener, property).orElse(defaultValue);
    }

    private OptionalInt getListenerSpecificIntProperty(final SupportedListener listener, final String property) {
        return config.readInt(target, getCompositePropertyName(listener, property));
    }

    private boolean allowGlobal(final SupportedListener listener, final SessionInfo sessionInfo) {
        final boolean override = listener.overrideGlobalGag();
        return override || notifications.allow(sessionInfo);
    }

    private boolean shouldNotify(final SupportedListener listener, final SessionInfo sessionInfo) {
        final boolean global = allowGlobal(listener, sessionInfo);
        return global && getSpecificCounter(listener).allow(sessionInfo);
    }

    private int getHourlyLimit() {
        final int val = config.readInt(target, HOURLY_LIMIT, Integer.MAX_VALUE);
        return (val < 0) ? Integer.MAX_VALUE : val;
    }

    private synchronized Counter getSpecificCounter(final SupportedListener listener) {
        return specificNotifications.computeIfAbsent(listener, key -> new Counter(this.getClass().getSimpleName(),
                                                                                  getHourlyLimit(key)));
    }

    boolean isEnabled() {
        return config.readBoolean(target, "enabled", false);
    }

    boolean isEnabled(final SupportedListener listener) {
        if (listener == SupportedListener.TESTING) {
            return true;
        } else {
            final String propName = getCompositePropertyName(listener, "enabled");
            return this.isEnabled() && config.readBoolean(target, propName, false);
        }
    }

    public void offer(final Submission s) throws Exception {
        LOGGER.trace("Received submission.");
        final SupportedListener listener = s.getSupportedListener();
        final SessionInfo session = s.getSessionInfo();
        if (!shouldNotify(listener, session)) {
            LOGGER.debug("Will not notify.");
            return;
        }
        final Map<String, Object> data = s.getData();
        LOGGER.trace("Triggering.");
        send(session, s.getSubject(), s.getMessage(data), s.getFallbackMessage(data));
        LOGGER.trace("Triggered.");
        getSpecificCounter(listener).increase(session);
        notifications.increase(session);
        LOGGER.trace("Finished.");
    }

    public abstract void send(final SessionInfo sessionInfo, final String subject,
                              final String message, final String fallbackMessage) throws Exception;
}
