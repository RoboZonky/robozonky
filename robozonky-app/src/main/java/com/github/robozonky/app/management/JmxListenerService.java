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

package com.github.robozonky.app.management;

import java.util.Objects;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.notifications.SellingStartedEvent;

public class JmxListenerService implements ListenerService {

    private static <T extends Event> EventListener<T> newListener(final Class<T> eventType) {
        if (Objects.equals(eventType, ExecutionStartedEvent.class)) {
            final Portfolio bean = (Portfolio) MBean.PORTFOLIO.getImplementation();
            return (event, sessionInfo) -> bean.handle((ExecutionStartedEvent) event);
        } else if (Objects.equals(eventType, ExecutionCompletedEvent.class)) {
            return (event, sessionInfo) -> {
                final ExecutionCompletedEvent evt = (ExecutionCompletedEvent) event;
                ((Runtime) MBean.RUNTIME.getImplementation()).handle(evt, sessionInfo);
                ((Portfolio) MBean.PORTFOLIO.getImplementation()).handle(evt);
            };
        } else if (Objects.equals(eventType, SellingStartedEvent.class)) {
            final Portfolio bean = (Portfolio) MBean.PORTFOLIO.getImplementation();
            return (event, sessionInfo) -> bean.handle((SellingStartedEvent) event);
        } else if (Objects.equals(eventType, SellingCompletedEvent.class)) {
            final Portfolio bean = (Portfolio) MBean.PORTFOLIO.getImplementation();
            return (event, sessionInfo) -> bean.handle((SellingCompletedEvent) event);
        } else if (Objects.equals(eventType, PurchasingStartedEvent.class)) {
            final Portfolio bean = (Portfolio) MBean.PORTFOLIO.getImplementation();
            return (event, sessionInfo) -> bean.handle((PurchasingStartedEvent) event);
        } else if (Objects.equals(eventType, PurchasingCompletedEvent.class)) {
            final Portfolio bean = (Portfolio) MBean.PORTFOLIO.getImplementation();
            return (event, sessionInfo) -> bean.handle((PurchasingCompletedEvent) event);
        } else if (Objects.equals(eventType, InvestmentDelegatedEvent.class)) {
            final Operations bean = (Operations) MBean.OPERATIONS.getImplementation();
            return (event, sessionInfo) -> bean.handle((InvestmentDelegatedEvent) event);
        } else if (Objects.equals(eventType, InvestmentRejectedEvent.class)) {
            final Operations bean = (Operations) MBean.OPERATIONS.getImplementation();
            return (event, sessionInfo) -> bean.handle((InvestmentRejectedEvent) event);
        } else if (Objects.equals(eventType, InvestmentMadeEvent.class)) {
            final Operations bean = (Operations) MBean.OPERATIONS.getImplementation();
            return (event, sessionInfo) -> bean.handle((InvestmentMadeEvent) event);
        } else if (Objects.equals(eventType, SaleOfferedEvent.class)) {
            final Operations bean = (Operations) MBean.OPERATIONS.getImplementation();
            return (event, sessionInfo) -> bean.handle((SaleOfferedEvent) event);
        } else if (Objects.equals(eventType, InvestmentPurchasedEvent.class)) {
            final Operations bean = (Operations) MBean.OPERATIONS.getImplementation();
            return (event, sessionInfo) -> bean.handle((InvestmentPurchasedEvent) event);
        } else {
            return null;
        }
    }

    @Override
    public <T extends Event> Refreshable<EventListener<T>> findListener(final Class<T> eventType) {
        final EventListener<T> listener = JmxListenerService.newListener(eventType);
        if (listener == null) {
            return null;
        }
        return Refreshable.createImmutable(listener);
    }
}
