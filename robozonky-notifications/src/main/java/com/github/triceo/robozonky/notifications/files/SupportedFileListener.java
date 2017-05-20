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
import com.github.triceo.robozonky.notifications.SupportedListener;

enum SupportedFileListener implements SupportedListener<FileNotificationProperties> {

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
    };

    protected abstract EventListener<? extends Event> newListener(final ListenerSpecificNotificationProperties properties);

    public EventListener<? extends Event> getListener(final FileNotificationProperties properties) {
        return this.newListener(new ListenerSpecificNotificationProperties(this, properties));
    }

}
