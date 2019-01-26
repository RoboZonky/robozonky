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

package com.github.robozonky.common.management;

import java.lang.management.ManagementFactory;
import java.util.Optional;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import io.vavr.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Management {

    private static final Logger LOGGER = LogManager.getLogger(Management.class);
    private static final Lazy<MBeanServer> server = Lazy.of(ManagementFactory::getPlatformMBeanServer);

    private Management() {
        // no instances
    }

    public static Optional<ObjectName> register(final ManagementBean<?> mbean) {
        try {
            final ObjectName name = mbean.getObjectName();
            LOGGER.debug("Registering MBean '{}'.", name);
            server.get().registerMBean(mbean.getInstance(), name);
            LOGGER.debug("Registered MBean '{}'.", name);
            return Optional.of(name);
        } catch (final NotCompliantMBeanException | InstanceAlreadyExistsException |
                MBeanRegistrationException ex) {
            LOGGER.warn("Failed registering MBean.", ex);
            return Optional.empty();
        }
    }

    public static void unregister(final ObjectName mbean) {
        LOGGER.debug("Unregistering MBean '{}'.", mbean);
        try {
            server.get().unregisterMBean(mbean);
            LOGGER.debug("Unregistered MBean '{}'.", mbean);
        } catch (final InstanceNotFoundException | MBeanRegistrationException ex) {
            LOGGER.info("Failed unregistering MBean.", ex);
        }
    }

    public static void unregisterAll() {
        LOGGER.debug("Unregistering MBeans.");
        server.get()
                .queryNames(null, new RoboZonkyQueryExp())
                .forEach(Management::unregister);
        LOGGER.debug("MBeans unregistered.");
    }
}
