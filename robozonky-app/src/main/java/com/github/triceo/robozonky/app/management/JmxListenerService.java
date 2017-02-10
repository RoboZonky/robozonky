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
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.ListenerService;
import com.github.triceo.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.triceo.robozonky.api.notifications.StrategyCompletedEvent;
import com.github.triceo.robozonky.api.notifications.StrategyStartedEvent;

public class JmxListenerService implements ListenerService {

    private static <T extends Event> EventListener<T> newListener(final Class<T> eventType) {
        if (Objects.equals(eventType, ExecutionCompletedEvent.class)) {
            return event -> {
                final ExecutionCompletedEvent evt = (ExecutionCompletedEvent)event;
                ((Runtime)MBean.RUNTIME.getImplementation()).registerInvestmentRun(evt);
                ((Investments)MBean.INVESTMENTS.getImplementation()).registerInvestmentRun(evt);
            };
        } else if (Objects.equals(eventType, InvestmentDelegatedEvent.class)) {
            final Investments bean = (Investments)MBean.INVESTMENTS.getImplementation();
            return event -> bean.addDelegatedInvestment((InvestmentDelegatedEvent)event);
        } else if (Objects.equals(eventType, InvestmentRejectedEvent.class)) {
            final Investments bean = (Investments)MBean.INVESTMENTS.getImplementation();
            return event -> bean.addRejectedInvestment((InvestmentRejectedEvent)event);
        } else if (Objects.equals(eventType, InvestmentMadeEvent.class)) {
            final Investments bean = (Investments)MBean.INVESTMENTS.getImplementation();
            return event -> bean.addSuccessfulInvestment((InvestmentMadeEvent) event);
        } else if (Objects.equals(eventType, RoboZonkyInitializedEvent.class)) {
            final Runtime bean = (Runtime) MBean.RUNTIME.getImplementation();
            return event -> bean.registerInitialization((RoboZonkyInitializedEvent) event);
        } else if (Objects.equals(eventType, StrategyStartedEvent.class)) {
            final Portfolio bean = (Portfolio) MBean.PORTFOLIO.getImplementation();
            return event -> bean.setPortfolioOverview((StrategyStartedEvent) event);
        } else if (Objects.equals(eventType, StrategyCompletedEvent.class)) {
            final Portfolio bean = (Portfolio) MBean.PORTFOLIO.getImplementation();
            return event -> bean.setPortfolioOverview((StrategyCompletedEvent) event);
        } else {
            return null;
        }
    }

    @Override
    public <T extends Event> Refreshable<EventListener<T>> findListener(final Class<T> eventType) {
        return Refreshable.createImmutable(JmxListenerService.newListener(eventType));
    }

}
