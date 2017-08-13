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

package com.github.triceo.robozonky.app.management;

import java.util.Objects;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.triceo.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.ListenerService;
import com.github.triceo.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.triceo.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.triceo.robozonky.api.notifications.SaleOfferedEvent;

public class JmxListenerService implements ListenerService {

    private static <T extends Event> EventListener<T> newListener(final Class<T> eventType) {
        if (Objects.equals(eventType, ExecutionCompletedEvent.class)) {
            return (event, sessionInfo) -> {
                final ExecutionCompletedEvent evt = (ExecutionCompletedEvent) event;
                ((Runtime) MBean.RUNTIME.getImplementation()).registerInvestmentRun(evt, sessionInfo);
            };
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
        } else if (Objects.equals(eventType, LoanNowDelinquentEvent.class)) {
            final Delinquency bean = (Delinquency) MBean.DELINQUENCY.getImplementation();
            return (event, sessionInfo) -> bean.handle((LoanNowDelinquentEvent) event);
        } else if (Objects.equals(eventType, LoanNoLongerDelinquentEvent.class)) {
            final Delinquency bean = (Delinquency) MBean.DELINQUENCY.getImplementation();
            return (event, sessionInfo) -> bean.handle((LoanNoLongerDelinquentEvent) event);
        } else if (Objects.equals(eventType, ExecutionStartedEvent.class)) {
            final Portfolio bean = (Portfolio) MBean.PORTFOLIO.getImplementation();
            return (event, sessionInfo) -> bean.setPortfolioOverview((ExecutionStartedEvent) event);
        } else if (Objects.equals(eventType, ExecutionCompletedEvent.class)) {
            final Portfolio bean = (Portfolio) MBean.PORTFOLIO.getImplementation();
            return (event, sessionInfo) -> bean.setPortfolioOverview((ExecutionCompletedEvent) event);
        } else {
            return null;
        }
    }

    @Override
    public <T extends Event> Refreshable<EventListener<T>> findListener(final Class<T> eventType) {
        return Refreshable.createImmutable(JmxListenerService.newListener(eventType));
    }
}
