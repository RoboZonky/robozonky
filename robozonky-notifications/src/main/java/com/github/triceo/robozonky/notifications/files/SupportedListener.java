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

package com.github.triceo.robozonky.notifications.files;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentSkippedEvent;

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
