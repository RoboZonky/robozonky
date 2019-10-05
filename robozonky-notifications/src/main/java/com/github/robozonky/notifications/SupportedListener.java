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
import com.github.robozonky.notifications.listeners.*;
import com.github.robozonky.notifications.samples.*;
import io.vavr.Lazy;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("rawtypes")
public enum SupportedListener {

    INVESTMENT_MADE {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new InvestmentMadeEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyInvestmentMadeEvent();
        }
    },
    INVESTMENT_SOLD {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new InvestmentSoldEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyInvestmentSoldEvent();
        }
    },
    INVESTMENT_PURCHASED {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new InvestmentPurchasedEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyInvestmentPurchasedEvent();
        }
    },
    SALE_OFFERED {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new SaleOfferedEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MySaleOfferedEvent();
        }
    },
    RESERVATION_ACCEPTED {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new ReservationAcceptedEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyReservationAcceptedEvent();
        }
    },
    LOAN_NOW_DELINQUENT {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDelinquentEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyLoanNowDelinquentEvent();
        }
    },
    LOAN_DELINQUENT_10_PLUS {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDelinquentEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyLoanDelinquent10DaysOrMoreEvent();
        }
    },
    LOAN_DELINQUENT_30_PLUS {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDelinquentEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyLoanDelinquent30DaysOrMoreEvent();
        }
    },
    LOAN_DELINQUENT_60_PLUS {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDelinquentEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyLoanDelinquent60DaysOrMoreEvent();
        }
    },
    LOAN_DELINQUENT_90_PLUS {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDelinquentEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyLoanDelinquent90DaysOrMoreEvent();
        }
    },
    LOAN_NO_LONGER_DELINQUENT {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanNoLongerDelinquentEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyLoanNoLongerDelinquentEvent();
        }
    },
    LOAN_DEFAULTED {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanDefaultedEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyLoanDefaultedEvent();
        }
    },
    LOAN_LOST {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new LoanLostEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyLoanLostEvent();
        }
    },
    WEEKLY_SUMMARY {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new WeeklySummaryEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyWeeklySummaryEvent();
        }

        @Override
        public boolean overrideGlobalGag() {
            return true; // weekly summary should always be sent
        }
    },
    CRASHED {
        @Override
        public boolean overrideGlobalGag() {
            return true;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyCrashedEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyRoboZonkyCrashedEvent();
        }
    },
    DAEMON_SUSPENDED {
        @Override
        public boolean overrideGlobalGag() {
            return true;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyDaemonSuspendedEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyRoboZonkyDaemonSuspendedEvent();
        }
    },
    DAEMON_RESUMED {
        @Override
        public boolean overrideGlobalGag() {
            return true;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyDaemonResumedEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyRoboZonkyDaemonResumedEvent();
        }
    },
    INITIALIZED {
        @Override
        public boolean overrideGlobalGag() {
            return true;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyInitializedEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyRoboZonkyInitializedEvent();
        }
    },
    ENDING {
        @Override
        public boolean overrideGlobalGag() {
            return true;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyEndingEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyRoboZonkyEndingEvent();
        }
    },
    TESTING {
        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyTestingEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyRoboZonkyTestingEvent();
        }
    },
    UPDATE_DETECTED {
        @Override
        public boolean overrideGlobalGag() {
            return true;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyUpdateDetectedEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyRoboZonkyUpdateDetectedEvent();
        }
    },
    EXPERIMENTAL_UPDATE_DETECTED {
        @Override
        public boolean overrideGlobalGag() {
            return true;
        }

        @Override
        public EventListener getListener(final AbstractTargetHandler targetHandler) {
            return new RoboZonkyExperimentalUpdateDetectedEventListener(this, targetHandler);
        }

        @Override
        protected Event createSampleEvent() {
            return new MyRoboZonkyExperimentalUpdateDetectedEvent();
        }
    };

    private final Lazy<? extends Event> sampleEvent = Lazy.of(this::createSampleEvent);

    public abstract EventListener getListener(final AbstractTargetHandler targetHandler);

    /**
     * Return ID of the listener. If listeners have the same ID, it means they share one namespace in configuration.
     *
     * @return ID of the listener which will be used as namespace in the config file.
     */
    public String getLabel() {
        final String interfaceName = this.getSampleEvent().getClass().getInterfaces()[0].getSimpleName();
        final String decapitalized = StringUtils.uncapitalize(interfaceName);
        // this works because Event subclasses must be named (Something)Event; check Event().
        return decapitalized.substring(0, decapitalized.length() - "Event".length());
    }

    protected abstract Event createSampleEvent();

    public Event getSampleEvent() {
        return sampleEvent.get();
    }

    /**
     * Whether or not the listener will ignore global anti-spam settings. The reason for this is that some very
     * important notifications can be ignored due to some other notification already exceeding the global e-mail
     * allowance. This should only be allowed for the most important notifications.
     *
     * @return True if global anti-spam settings will be ignored for this notification.
     */
    public boolean overrideGlobalGag() {
        return false;
    }

}

