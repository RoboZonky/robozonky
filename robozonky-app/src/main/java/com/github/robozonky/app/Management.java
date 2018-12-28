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

package com.github.robozonky.app;

import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import com.github.robozonky.common.extensions.ManagementBeanServiceLoader;
import com.github.robozonky.common.management.BaseMBean;
import com.github.robozonky.common.management.ManagementBean;
import io.vavr.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Management implements ShutdownHook.Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Management.class);

    private static final Lazy<MBeanServer> server = Lazy.of(ManagementFactory::getPlatformMBeanServer);

    private static Optional<ObjectName> register(final ManagementBean<?> mbean) {
        try {
            final BaseMBean impl = mbean.getInstance();
            final ObjectName name = mbean.getObjectName();
            server.get().registerMBean(impl, name);
            LOGGER.debug("Registered MBean '{}'.", name);
            return Optional.of(name);
        } catch (final NotCompliantMBeanException | InstanceAlreadyExistsException |
                MBeanRegistrationException ex) {
            LOGGER.warn("Failed loading MBean.", ex);
            return Optional.empty();
        }
    }

    private static Set<ObjectName> registerAll() {
        return ManagementBeanServiceLoader.loadManagementBeans()
                .map(Management::register)
                .flatMap(n -> n.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toSet());
    }

    private static void unregisterAll(final Set<ObjectName> mbeans) {
        mbeans.forEach(name -> {
            try {
                server.get().unregisterMBean(name);
                LOGGER.debug("Unregistered MBean '{}'.", name);
            } catch (final InstanceNotFoundException | MBeanRegistrationException ex) {
                LOGGER.info("Failed unloading MBean.", ex);
            }
        });
    }

    @Override
    public Optional<Consumer<ShutdownHook.Result>> get() {
        LOGGER.debug("Registering MBeans.");
        final Set<ObjectName> mbeans = Management.registerAll();
        LOGGER.debug("MBeans registered.");
        return Optional.of(result -> {
            LOGGER.debug("Unregistering MBeans.");
            Management.unregisterAll(mbeans);
            LOGGER.debug("MBeans unregistered.");
        });
    }
}
