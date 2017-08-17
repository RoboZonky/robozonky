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

package com.github.triceo.robozonky.app.management;

import java.lang.management.ManagementFactory;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum MBean {

    RUNTIME(Runtime::new),
    OPERATIONS(Operations::new),
    DELINQUENCY(Delinquency::new),
    PORTFOLIO(Portfolio::new);

    private static final Logger LOGGER = LoggerFactory.getLogger(MBean.class);

    public static void loadAll() {
        MBean.LOGGER.debug("Registering MBeans.");
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Stream.of(MBean.values())
                .map(MBean::getImplementation)
                .forEach(i -> {
                    try {
                        final ObjectName name = MBean.assembleObjectName(i);
                        server.registerMBean(i, name);
                        MBean.LOGGER.debug("Registered MBean '{}'.", name);
                    } catch (final NotCompliantMBeanException | InstanceAlreadyExistsException |
                            MBeanRegistrationException ex) {
                        MBean.LOGGER.warn("Failed loading MBean.", ex);
                    }
                });
        MBean.LOGGER.debug("MBeans registered.");
    }

    public static void unloadAll() {
        MBean.LOGGER.debug("Unregistering MBeans.");
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Stream.of(MBean.values())
                .map(MBean::getImplementation)
                .map(MBean::assembleObjectName)
                .forEach(name -> {
                    try {
                        server.unregisterMBean(name);
                        MBean.LOGGER.debug("Unregistered MBean '{}'.", name);
                    } catch (final InstanceNotFoundException | MBeanRegistrationException ex) {
                        MBean.LOGGER.info("Failed unloading MBean.", ex);
                    }
                });
        MBean.LOGGER.debug("MBeans unregistered.");
    }

    static ObjectName assembleObjectName(final BaseMBean implementation) {
        try {
            final String className = implementation.getClass().getSimpleName();
            return new ObjectName("com.github.triceo.robozonky:type=" + className);
        } catch (final MalformedObjectNameException ex) {
            MBean.LOGGER.warn("MBean '{}' will be ignored.", implementation.getClass(), ex);
            return null;
        }
    }

    private final Supplier<BaseMBean> supplier;
    private BaseMBean implementation;

    MBean(final Supplier<BaseMBean> supplier) {
        this.supplier = supplier;
        this.reset();
    }

    public BaseMBean getImplementation() {
        return implementation;
    }

    void reset() {
        implementation = supplier.get();
    }
}
