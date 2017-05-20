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
import com.github.triceo.robozonky.api.notifications.RoboZonkyExperimentalUpdateDetectedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyUpdateDetectedEvent;
import com.github.triceo.robozonky.notifications.SupportedListener;

enum SupportedEmailListener implements SupportedListener<EmailNotificationProperties> {

    INVESTMENT_MADE {
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
        public Class<? extends Event> getEventType() {
            return InvestmentSkippedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new InvestmentSkippedEventListener(properties);
        }
    }, INVESTMENT_DELEGATED {
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
        public Class<? extends Event> getEventType() {
            return RoboZonkyCrashedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyCrashedEventListener(properties);
        }
    }, DAEMON_FAILED {
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
        public Class<? extends Event> getEventType() {
            return RemoteOperationFailedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RemoteOperationFailedEventListener(properties);
        }
    }, INITIALIZED {
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
        public Class<? extends Event> getEventType() {
            return RoboZonkyEndingEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyEndingEventListener(properties);
        }
    }, TESTING {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyTestingEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyTestingEventListener(properties);
        }
    }, UPDATE_DETECTED {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyUpdateDetectedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyUpdateDetectedEventListener(properties);
        }
    }, EXPERIMENTAL_UPDATE_DETECTED {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyExperimentalUpdateDetectedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyExperimentalUpdateDetectedEventListener(properties);
        }
    };

    protected abstract EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties);

    @Override
    public EventListener<? extends Event> getListener(final EmailNotificationProperties properties) {
        return this.newListener(new ListenerSpecificNotificationProperties(this, properties));
    }

}
