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

package com.github.robozonky.notifications;

import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractTargetHandler {

    private static final String HOURLY_LIMIT = "hourlyMaxEmails";
    protected final Target target;
    final ConfigStorage config;
    private final Logger LOGGER = LogManager.getLogger(getClass());
    private final Map<SessionInfo, Counter> notifications = new HashMap<>(0);
    private final Map<SupportedListener, Map<SessionInfo, Counter>> specificNotifications =
            new EnumMap<>(SupportedListener.class);

    protected AbstractTargetHandler(final ConfigStorage config, final Target target) {
        this.config = config;
        this.target = target;
    }

    private static String getCompositePropertyName(final SupportedListener listener, final String property) {
        return listener.getLabel() + "." + property;
    }

    private synchronized Counter getCounter(final SessionInfo sessionInfo) {
        return notifications.computeIfAbsent(sessionInfo,
                                             s -> new Counter(s, "global", getHourlyLimit(), Duration.ofHours(1)));
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
        return override || getCounter(sessionInfo).allow();
    }

    private boolean shouldNotify(final SupportedListener listener, final SessionInfo sessionInfo) {
        final boolean global = allowGlobal(listener, sessionInfo);
        return global && getSpecificCounter(sessionInfo, listener).allow();
    }

    private int getHourlyLimit() {
        final int val = config.readInt(target, HOURLY_LIMIT, Integer.MAX_VALUE);
        return (val < 0) ? Integer.MAX_VALUE : val;
    }

    private synchronized Counter getSpecificCounter(final SessionInfo sessionInfo, final SupportedListener listener) {
        return specificNotifications.computeIfAbsent(listener, key -> new HashMap<>(1))
                .computeIfAbsent(sessionInfo, s -> new Counter(s, this.getClass().getSimpleName(),
                                                               getHourlyLimit(listener)));
    }

    boolean isEnabledInSettings() {
        return config.readBoolean(target, "enabled", false);
    }

    private boolean isEnabledInSettings(final SupportedListener listener) {
        final String propName = getCompositePropertyName(listener, "enabled");
        return this.isEnabledInSettings() && config.readBoolean(target, propName, false);
    }

    private boolean enableNoLongerDelinquentNotifications() {
        /*
         * "no longer delinquent" will only be triggered in case a loan was previously marked as delinquent - those are
         * "companion" notifications, the first making no sense without the second. therefore we enable it in case
         * any of the others is enabled as well. it can not be disabled.
         */
        return Stream.of(SupportedListener.LOAN_NOW_DELINQUENT, SupportedListener.LOAN_DELINQUENT_10_PLUS,
                         SupportedListener.LOAN_DELINQUENT_30_PLUS, SupportedListener.LOAN_DELINQUENT_60_PLUS,
                         SupportedListener.LOAN_DELINQUENT_90_PLUS)
                .anyMatch(this::isEnabled);
    }

    boolean isEnabled(final SupportedListener listener) {
        final boolean noLongerDelinquentEnabled = listener == SupportedListener.LOAN_NO_LONGER_DELINQUENT &&
                enableNoLongerDelinquentNotifications();
        if (noLongerDelinquentEnabled || listener == SupportedListener.TESTING) {
            // testing is always enabled so that notification testing in the installer has something to work with
            return true;
        } else {
            return isEnabledInSettings(listener);
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
        getSpecificCounter(session, listener).increase();
        getCounter(session).increase();
        LOGGER.trace("Finished.");
    }

    public abstract void send(final SessionInfo sessionInfo, final String subject,
                              final String message, final String fallbackMessage) throws Exception;
}
