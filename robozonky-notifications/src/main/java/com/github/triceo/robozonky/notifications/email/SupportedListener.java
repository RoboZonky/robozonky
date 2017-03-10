/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications.email;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.triceo.robozonky.api.notifications.RemoteOperationFailedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyTestingEvent;

enum SupportedListener {

    INVESTMENT_MADE {
        @Override
        public String getLabel() {
            return "investmentMade";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentMadeEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new InvestmentMadeEventListener(properties);
        }
    }, INVESTMENT_SKIPPED {
        @Override
        public String getLabel() {
            return "investmentSkipped";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentSkippedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new InvestmentSkippedEventListener(properties);
        }
    }, INVESTMENT_DELEGATED {
        @Override
        public String getLabel() {
            return "investmentDelegated";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentDelegatedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new InvestmentDelegatedEventListener(properties);
        }
    }, INVESTMENT_REJECTED {
        @Override
        public String getLabel() {
            return "investmentRejected";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentRejectedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new InvestmentRejectedEventListener(properties);
        }
    }, BALANCE_ON_TARGET {
        @Override
        public String getLabel() {
            return "balanceTracker";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return ExecutionStartedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new BalanceOnTargetEventListener(properties);
        }
    }, BALANCE_UNDER_MINIMUM {
        @Override
        public String getLabel() {
            return "balanceTracker";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return ExecutionStartedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new BalanceUnderMinimumEventListener(properties);
        }
    }, CRASHED {
        @Override
        public String getLabel() {
            return "roboZonkyCrashed";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyCrashedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyCrashedEventListener(properties);
        }
    }, DAEMON_FAILED {
        @Override
        public String getLabel() {
            return "roboZonkyDaemonFailed";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyDaemonFailedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyDaemonFailedEventListener(properties);
        }
    }, REMOTE_OPERATION_FAILED {
        @Override
        public String getLabel() {
            return "remoteOperationFailed";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return RemoteOperationFailedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RemoteOperationFailedEventListener(properties);
        }
    }, INITIALIZED {
        @Override
        public String getLabel() {
            return "roboZonkyInitialized";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyInitializedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyInitializedEventListener(properties);
        }
    }, ENDING {
        @Override
        public String getLabel() {
            return "roboZonkyEnding";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyEndingEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyEndingEventListener(properties);
        }
    }, TESTING {
        @Override
        public String getLabel() {
            return "roboZonkyTesting";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyTestingEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyTestingEventListener(properties);
        }
    };

    /**
     * Return ID of the listener. If listeners have the same ID, it means they share one namespace in configuration.
     *
     * @return ID of the listener which will be used as namespace in the config file.
     */
    public abstract String getLabel();

    /**
     * Type of event that this listener responds to.
     *
     * @return Event type.
     */
    public abstract Class<? extends Event> getEventType();

    protected abstract EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties);

    public EventListener<? extends Event> getListener(final NotificationProperties properties) {
        return this.newListener(new ListenerSpecificNotificationProperties(this, properties));
    }

}
