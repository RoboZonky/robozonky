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

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanLostEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.notifications.LoanRepaidEvent;
import com.github.robozonky.api.notifications.ReservationAcceptedEvent;
import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyExperimentalUpdateDetectedEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.api.notifications.RoboZonkyUpdateDetectedEvent;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.notifications.listeners.BalanceOnTargetEventListener;
import com.github.robozonky.notifications.listeners.BalanceUnderMinimumEventListener;
import com.github.robozonky.notifications.listeners.InvestmentDelegatedEventListener;
import com.github.robozonky.notifications.listeners.InvestmentMadeEventListener;
import com.github.robozonky.notifications.listeners.InvestmentPurchasedEventListener;
import com.github.robozonky.notifications.listeners.InvestmentRejectedEventListener;
import com.github.robozonky.notifications.listeners.InvestmentSkippedEventListener;
import com.github.robozonky.notifications.listeners.InvestmentSoldEventListener;
import com.github.robozonky.notifications.listeners.LoanDefaultedEventListener;
import com.github.robozonky.notifications.listeners.LoanDelinquentEventListener;
import com.github.robozonky.notifications.listeners.LoanLostEventListener;
import com.github.robozonky.notifications.listeners.LoanNoLongerDelinquentEventListener;
import com.github.robozonky.notifications.listeners.LoanRepaidEventListener;
import com.github.robozonky.notifications.listeners.ReservationAcceptedEventListener;
import com.github.robozonky.notifications.listeners.RoboZonkyDaemonFailedEventListener;
import com.github.robozonky.notifications.listeners.RoboZonkyEndingEventListener;
import com.github.robozonky.notifications.listeners.RoboZonkyExperimentalUpdateDetectedEventListener;
import com.github.robozonky.notifications.listeners.RoboZonkyInitializedEventListener;
import com.github.robozonky.notifications.listeners.RoboZonkyTestingEventListener;
import com.github.robozonky.notifications.listeners.RoboZonkyUpdateDetectedEventListener;
import com.github.robozonky.notifications.listeners.SaleOfferedEventListener;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("rawtypes")
public enum SupportedListener {

    INVESTMENT_MADE {
        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentMadeEvent.class;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new InvestmentMadeEventListener(this, targetHandler);
        }
    },
    INVESTMENT_SOLD {
        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentSoldEvent.class;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new InvestmentSoldEventListener(this, targetHandler);
        }
    },
    INVESTMENT_SKIPPED {
        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentSkippedEvent.class;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new InvestmentSkippedEventListener(this, targetHandler);
        }
    },
    INVESTMENT_DELEGATED {
        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentDelegatedEvent.class;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new InvestmentDelegatedEventListener(this, targetHandler);
        }
    },
    INVESTMENT_REJECTED {
        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentRejectedEvent.class;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new InvestmentRejectedEventListener(this, targetHandler);
        }
    },
    INVESTMENT_PURCHASED {
        @Override
        public Class<? extends Event> getEventType() {
            return InvestmentPurchasedEvent.class;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new InvestmentPurchasedEventListener(this, targetHandler);
        }
    },
    SALE_OFFERED {
        @Override
        public Class<? extends Event> getEventType() {
            return SaleOfferedEvent.class;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new SaleOfferedEventListener(this, targetHandler);
        }
    },
    RESERVATION_ACCEPTED {
        @Override
        public Class<? extends Event> getEventType() {
            return ReservationAcceptedEvent.class;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new ReservationAcceptedEventListener(this, targetHandler);
        }
    },
    LOAN_NOW_DELINQUENT {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDelinquentEventListener(this, targetHandler);
        }

        @Override
        public Class<? extends Event> getEventType() {
            return LoanNowDelinquentEvent.class;
        }
    },
    LOAN_DELINQUENT_10_PLUS {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDelinquentEventListener(this, targetHandler);
        }

        @Override
        public Class<? extends Event> getEventType() {
            return LoanDelinquent10DaysOrMoreEvent.class;
        }
    },
    LOAN_DELINQUENT_30_PLUS {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDelinquentEventListener(this, targetHandler);
        }

        @Override
        public Class<? extends Event> getEventType() {
            return LoanDelinquent30DaysOrMoreEvent.class;
        }
    },
    LOAN_DELINQUENT_60_PLUS {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDelinquentEventListener(this, targetHandler);
        }

        @Override
        public Class<? extends Event> getEventType() {
            return LoanDelinquent60DaysOrMoreEvent.class;
        }
    },
    LOAN_DELINQUENT_90_PLUS {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDelinquentEventListener(this, targetHandler);
        }

        @Override
        public Class<? extends Event> getEventType() {
            return LoanDelinquent90DaysOrMoreEvent.class;
        }
    },
    LOAN_NO_LONGER_DELINQUENT {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanNoLongerDelinquentEventListener(this, targetHandler);
        }

        @Override
        public Class<? extends Event> getEventType() {
            return LoanNoLongerDelinquentEvent.class;
        }
    },
    LOAN_DEFAULTED {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDefaultedEventListener(this, targetHandler);
        }

        @Override
        public Class<? extends Event> getEventType() {
            return LoanDefaultedEvent.class;
        }
    },
    LOAN_LOST {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanLostEventListener(this, targetHandler);
        }

        @Override
        public Class<? extends Event> getEventType() {
            return LoanLostEvent.class;
        }
    },
    LOAN_REPAID {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanRepaidEventListener(this, targetHandler);
        }

        @Override
        public Class<? extends Event> getEventType() {
            return LoanRepaidEvent.class;
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
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new BalanceOnTargetEventListener(this, targetHandler);
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
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new BalanceUnderMinimumEventListener(this, targetHandler);
        }
    },
    DAEMON_FAILED {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyDaemonFailedEvent.class;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyDaemonFailedEventListener(this, targetHandler);
        }
    },
    INITIALIZED {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyInitializedEvent.class;
        }

        @Override
        public boolean overrideGlobalGag() {
            return true;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyInitializedEventListener(this, targetHandler);
        }
    },
    ENDING {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyEndingEvent.class;
        }

        @Override
        public boolean overrideGlobalGag() {
            return true;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyEndingEventListener(this, targetHandler);
        }
    },
    TESTING {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyTestingEvent.class;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyTestingEventListener(this, targetHandler);
        }
    },
    UPDATE_DETECTED {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyUpdateDetectedEvent.class;
        }

        @Override
        public boolean overrideGlobalGag() {
            return true;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyUpdateDetectedEventListener(this, targetHandler);
        }
    },
    EXPERIMENTAL_UPDATE_DETECTED {
        @Override
        public Class<? extends Event> getEventType() {
            return RoboZonkyExperimentalUpdateDetectedEvent.class;
        }

        @Override
        public boolean overrideGlobalGag() {
            return true;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyExperimentalUpdateDetectedEventListener(this, targetHandler);
        }
    };

    public abstract EventListener getListener(final AbstractTargetHandler targetHandler);

    /**
     * Return ID of the listener. If listeners have the same ID, it means they share one namespace in configuration.
     * @return ID of the listener which will be used as namespace in the config file.
     */
    public String getLabel() {
        final String className = this.getEventType().getSimpleName();
        final String decapitalized = StringUtils.uncapitalize(className);
        // this works because Event subclasses must be named (Something)Event; check Event().
        return decapitalized.substring(0, decapitalized.length() - "Event".length());
    }

    /**
     * Type of event that this listener responds to.
     * @return Event type.
     */
    public abstract Class<? extends Event> getEventType();

    /**
     * Whether or not the listener will ignore global anti-spam settings. The reason for this is that some very
     * important notifications can be ignored due to some other notification already exceeding the global e-mail
     * allowance. This should only be allowed for the most important notifications.
     * @return True if global anti-spam settings will be ignored for this notification.
     */
    public boolean overrideGlobalGag() {
        return false;
    }

}
