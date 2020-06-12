/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.installer.configuration;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.installer.Util;

final class EnabledJmxConfiguration extends AbstractJmxConfiguration {

    private final boolean jmxSecurityEnabled;
    private final String jmxHostname;
    private final int jmxPort;
    private final AtomicReference<String> targetPropertyFile = new AtomicReference<>();

    public EnabledJmxConfiguration(final String jmxHostname, final int jmxPort, final boolean jmxSecurityEnabled) {
        super(true);
        this.jmxHostname = jmxHostname;
        this.jmxPort = jmxPort;
        this.jmxSecurityEnabled = jmxSecurityEnabled;
    }

    @Override
    public Map<String, String> getProperties() {
        SortedMap<String, String> result = new TreeMap<>(super.getProperties());
        result.put("com.sun.management.config.file", targetPropertyFile.get());
        result.put("java.rmi.server.hostname", jmxHostname);
        // The buffer is effectively a memory leak; we'll reduce its size from 1000 to 10.
        result.put("jmx.remote.x.notification.buffer.size", "10");
        return Collections.unmodifiableMap(result);
    }

    @Override
    public void accept(Path distributionRoot, Path installationRoot) {
        final Properties props = new Properties();
        props.setProperty("com.sun.management.jmxremote.authenticate", Boolean.toString(jmxSecurityEnabled));
        props.setProperty("com.sun.management.jmxremote.ssl", "false");
        props.setProperty("com.sun.management.jmxremote.rmi.port", Integer.toString(jmxPort));
        props.setProperty("com.sun.management.jmxremote.port", Integer.toString(jmxPort));
        Path target = installationRoot.resolve("management.properties")
            .toAbsolutePath();
        try {
            Util.writeOutProperties(props, target);
            targetPropertyFile.set(target.toString());
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed writing JMX configuration: " + target, ex);
        }
    }
}
