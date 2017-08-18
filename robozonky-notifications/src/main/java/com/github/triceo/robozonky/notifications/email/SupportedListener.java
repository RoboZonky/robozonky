/*
 * Copyright 2017 The RoboZonky Project
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
import com.github.triceo.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.triceo.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.triceo.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.triceo.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.triceo.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.triceo.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.triceo.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.triceo.robozonky.api.notifications.RemoteOperationFailedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyExperimentalUpdateDetectedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyUpdateDetectedEvent;
import com.github.triceo.robozonky.api.notifications.SaleOfferedEvent;
import org.apache.commons.lang3.StringUtils;

enum SupportedListener {

    INVESTMENT_MADE {
        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentMadeEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new InvestmentMadeEventListener(properties);
        }
    },
    INVESTMENT_SOLD {
        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentSoldEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new InvestmentSoldEventListener(properties);
        }
    },
    INVESTMENT_SKIPPED {
        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentSkippedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new InvestmentSkippedEventListener(properties);
        }
    },
    INVESTMENT_DELEGATED {
        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentDelegatedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new InvestmentDelegatedEventListener(properties);
        }
    },
    INVESTMENT_REJECTED {
        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentRejectedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new InvestmentRejectedEventListener(properties);
        }
    },
    INVESTMENT_PURCHASED {
        @Override
        Class<? extends Event> getEventType() {
            return InvestmentPurchasedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new InvestmentPurchasedEventListener(properties);
        }
    },
    SALE_OFFERED {
        @Override
        Class<? extends Event> getEventType() {
            return SaleOfferedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new SaleOfferedEventListener(properties);
        }
    },
    LOAN_NOW_DELINQUENT {
        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new LoanDelinquentEventListener(properties);
        }

        @Override
        Class<? extends Event> getEventType() {
            return LoanNowDelinquentEvent.class;
        }
    },
    LOAN_DELINQUENT_10_PLUS {
        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new LoanDelinquentEventListener(properties);
        }

        @Override
        Class<? extends Event> getEventType() {
            return LoanDelinquent10DaysOrMoreEvent.class;
        }
    },
    LOAN_DELINQUENT_30_PLUS {
        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new LoanDelinquentEventListener(properties);
        }

        @Override
        Class<? extends Event> getEventType() {
            return LoanDelinquent30DaysOrMoreEvent.class;
        }
    },
    LOAN_DELINQUENT_60_PLUS {
        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new LoanDelinquentEventListener(properties);
        }

        @Override
        Class<? extends Event> getEventType() {
            return LoanDelinquent60DaysOrMoreEvent.class;
        }
    },
    LOAN_DELINQUENT_90_PLUS {
        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new LoanDelinquentEventListener(properties);
        }

        @Override
        Class<? extends Event> getEventType() {
            return LoanDelinquent90DaysOrMoreEvent.class;
        }
    },
    LOAN_NO_LONGER_DELINQUENT {
        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new LoanNoLongerDelinquentEventListener(properties);
        }

        @Override
        Class<? extends Event> getEventType() {
            return LoanNoLongerDelinquentEvent.class;
        }
    },
    BALANCE_ON_TARGET {
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
    },
    BALANCE_UNDER_MINIMUM {
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
    },
    CRASHED {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyCrashedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyCrashedEventListener(properties);
        }
    },
    DAEMON_FAILED {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyDaemonFailedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyDaemonFailedEventListener(properties);
        }
    },
    REMOTE_OPERATION_FAILED {
        @Override
        public Class<? extends Event> getEventType() {
            return RemoteOperationFailedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RemoteOperationFailedEventListener(properties);
        }
    },
    INITIALIZED {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyInitializedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyInitializedEventListener(properties);
        }
    },
    ENDING {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyEndingEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyEndingEventListener(properties);
        }
    },
    TESTING {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyTestingEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyTestingEventListener(properties);
        }
    },
    UPDATE_DETECTED {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyUpdateDetectedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyUpdateDetectedEventListener(properties);
        }
    },
    EXPERIMENTAL_UPDATE_DETECTED {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyExperimentalUpdateDetectedEvent.class;
        }

        @Override
        protected EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties) {
            return new RoboZonkyExperimentalUpdateDetectedEventListener(properties);
        }
    };

    protected abstract EventListener<? extends Event> newListener(
            final ListenerSpecificNotificationProperties properties);

    /**
     * Return ID of the listener. If listeners have the same ID, it means they share one namespace in configuration.
     * @return ID of the listener which will be used as namespace in the config file.
     */
    String getLabel() {
        final String className = this.getEventType().getSimpleName();
        final String decapitalized = StringUtils.uncapitalize(className);
        // this works because Event subclasses must be named (Something)Event; check Event().
        return decapitalized.substring(0, decapitalized.length() - "Event".length());
    }

    /**
     * Type of event that this listener responds to.
     * @return Event type.
     */
    abstract Class<? extends Event> getEventType();

    public EventListener<? extends Event> getListener(final NotificationProperties properties) {
        return this.newListener(new ListenerSpecificNotificationProperties(this, properties));
    }

}
