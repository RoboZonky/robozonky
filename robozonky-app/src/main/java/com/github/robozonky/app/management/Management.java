/*
 * Copyright 2018 The RoboZonky Project
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

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.runtime.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Management implements ShutdownHook.Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Management.class);
    private final Lifecycle lifecycle;

    public Management(final Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    private static Map<MBean, Object> loadAll(final Lifecycle lifecycle) {
        LOGGER.debug("Registering MBeans.");
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final Map<MBean, Object> instances = new EnumMap<>(MBean.class);
        for (final MBean mbean : MBean.values()) {
            try {
                final Object impl = mbean.newImplementation(lifecycle);
                server.registerMBean(impl, mbean.getObjectName());
                LOGGER.debug("Registered MBean '{}'.", mbean.getObjectName());
                instances.put(mbean, impl);
            } catch (final NotCompliantMBeanException | InstanceAlreadyExistsException |
                    MBeanRegistrationException ex) {
                LOGGER.warn("Failed loading MBean.", ex);
            }
        }
        LOGGER.debug("MBeans registered.");
        return instances;
    }

    private static void unloadAll() {
        LOGGER.debug("Unregistering MBeans.");
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Stream.of(MBean.values())
                .map(MBean::getObjectName)
                .forEach(name -> {
                    try {
                        server.unregisterMBean(name);
                        LOGGER.debug("Unregistered MBean '{}'.", name);
                    } catch (final InstanceNotFoundException | MBeanRegistrationException ex) {
                        LOGGER.info("Failed unloading MBean.", ex);
                    }
                });
        LOGGER.debug("MBeans unregistered.");
    }

    @Override
    public Optional<Consumer<ShutdownHook.Result>> get() {
        JmxListenerService.setInstances(Management.loadAll(lifecycle));
        return Optional.of(result -> {
            Management.unloadAll();
            JmxListenerService.setInstances(Collections.emptyMap());
        });
    }
}
