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

import java.lang.management.ManagementFactory;
import java.util.Optional;
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

    RUNTIME(Runtime::new);

    private static final Logger LOGGER = LoggerFactory.getLogger(MBean.class);

    public static void loadAll() {
        MBean.LOGGER.debug("Registering MBeans.");
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Stream.of(MBean.values())
                .filter(mbean -> mbean.getObjectName().isPresent())
                .forEach(mbean -> {
                    try {
                        final ObjectName name = mbean.getObjectName().get();
                        server.registerMBean(mbean.getImplementation(), name);
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
                .filter(mbean -> mbean.getObjectName().isPresent())
                .flatMap(mbean -> mbean.getObjectName().map(Stream::of).orElse(Stream.empty()))
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

    private final Object implementation;
    private ObjectName objectName;

    MBean(final Supplier<?> impl) {
        this.implementation = impl.get();
        try {
            final String className = this.implementation.getClass().getSimpleName();
            this.objectName = new ObjectName("com.github.triceo.robozonky:type=" + className);
        } catch (final MalformedObjectNameException ex) {
            LoggerFactory.getLogger(MBean.class).warn("MBean '{}' will be ignored.", this, ex);
            this.objectName = null;
        }
    }

    public Object getImplementation() {
        return implementation;
    }

    public Optional<ObjectName> getObjectName() {
        return Optional.ofNullable(objectName);
    }

}
