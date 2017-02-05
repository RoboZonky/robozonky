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
import com.github.triceo.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyInitializedEvent;

enum SupportedListener {

    INVESTMENT_MADE {
        @Override
        public String getId() {
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
    }, INVESTMENT_DELEGATED {
        @Override
        public String getId() {
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
        public String getId() {
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
    }, EXECUTION_STARTED {
        @Override
        public String getId() {
            return "balanceTracker";
        }

        @Override
        public Class<? extends Event> getEventType() {
            return ExecutionStartedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new ExecutionStartedEventListener(properties);
        }
    }, CRASHED {
        @Override
        public String getId() {
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
        public String getId() {
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
    }, INITIALIZED {
        @Override
        public String getId() {
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
        public String getId() {
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
    };

    public abstract String getId();

    public abstract Class<? extends Event> getEventType();

    protected abstract EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties);

    public EventListener<? extends Event> getListener(final NotificationProperties properties) {
        return this.newListener(new ListenerSpecificNotificationProperties(this, properties));
    }

}
