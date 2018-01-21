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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxListenerService implements ListenerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmxListenerService.class);
    private static Map<MBean, Object> IMPLEMENTATIONS = Collections.emptyMap();

    public static void setInstances(final Map<MBean, Object> implementations) {
        LOGGER.trace("Setting MBeans: {}.", implementations);
        IMPLEMENTATIONS = Collections.unmodifiableMap(implementations);
    }

    static Optional<Object> getMBean(final MBean type) {
        return Optional.ofNullable(IMPLEMENTATIONS.get(type));
    }

    private static void callOnRuntime(final Consumer<Runtime> operation) {
        final Optional<Object> mbean = getMBean(MBean.RUNTIME);
        if (mbean.isPresent()) {
            operation.accept((Runtime) mbean.get());
        } else {
            LOGGER.warn("Runtime MBean not found.");
        }
    }

    private static void callOnOperations(final Consumer<Operations> operation) {
        final Optional<Object> mbean = getMBean(MBean.OPERATIONS);
        if (mbean.isPresent()) {
            operation.accept((Operations) mbean.get());
        } else {
            LOGGER.warn("Operations MBean not found.");
        }
    }

    private static void callOnPortfolio(final Consumer<Portfolio> operation) {
        final Optional<Object> mbean = getMBean(MBean.PORTFOLIO);
        if (mbean.isPresent()) {
            operation.accept((Portfolio) mbean.get());
        } else {
            LOGGER.warn("Operations MBean not found.");
        }
    }

    private static <T extends Event> EventListener<T> newListener(final Class<T> eventType) {
        if (Objects.equals(eventType, ExecutionStartedEvent.class)) {
            return (event, sessionInfo) -> callOnPortfolio(bean -> bean.handle((ExecutionStartedEvent) event));
        } else if (Objects.equals(eventType, ExecutionCompletedEvent.class)) {
            return (event, sessionInfo) -> {
                final ExecutionCompletedEvent evt = (ExecutionCompletedEvent) event;
                callOnRuntime(bean -> bean.handle(evt, sessionInfo));
                callOnPortfolio(bean -> bean.handle(evt));
            };
        } else if (Objects.equals(eventType, SellingStartedEvent.class)) {
            return (event, sessionInfo) -> callOnPortfolio(bean -> bean.handle((SellingStartedEvent) event));
        } else if (Objects.equals(eventType, SellingCompletedEvent.class)) {
            return (event, sessionInfo) -> callOnPortfolio(bean -> bean.handle((SellingCompletedEvent) event));
        } else if (Objects.equals(eventType, PurchasingStartedEvent.class)) {
            return (event, sessionInfo) -> callOnPortfolio(bean -> bean.handle((PurchasingStartedEvent) event));
        } else if (Objects.equals(eventType, PurchasingCompletedEvent.class)) {
            return (event, sessionInfo) -> callOnPortfolio(bean -> bean.handle((PurchasingCompletedEvent) event));
        } else if (Objects.equals(eventType, InvestmentDelegatedEvent.class)) {
            return (event, sessionInfo) -> callOnOperations(bean -> bean.handle((InvestmentDelegatedEvent) event));
        } else if (Objects.equals(eventType, InvestmentRejectedEvent.class)) {
            return (event, sessionInfo) -> callOnOperations(bean -> bean.handle((InvestmentRejectedEvent) event));
        } else if (Objects.equals(eventType, InvestmentMadeEvent.class)) {
            return (event, sessionInfo) -> callOnOperations(bean -> bean.handle((InvestmentMadeEvent) event));
        } else if (Objects.equals(eventType, SaleOfferedEvent.class)) {
            return (event, sessionInfo) -> callOnOperations(bean -> bean.handle((SaleOfferedEvent) event));
        } else if (Objects.equals(eventType, InvestmentPurchasedEvent.class)) {
            return (event, sessionInfo) -> callOnOperations(bean -> bean.handle((InvestmentPurchasedEvent) event));
        } else {
            return null;
        }
    }

    @Override
    public <T extends Event> EventListenerSupplier<T> findListener(final Class<T> eventType) {
        final EventListener<T> listener = JmxListenerService.newListener(eventType);
        if (listener == null) {
            return null;
        }
        return () -> Optional.of(listener);
    }
}
