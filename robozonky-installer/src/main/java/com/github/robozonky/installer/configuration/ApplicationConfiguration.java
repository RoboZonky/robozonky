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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.github.robozonky.installer.Util;

final class ApplicationConfiguration implements PropertyConfiguration {

    private final boolean dryRunEnabled;
    private final char[] keystoreSecret;
    private final Path keystoreSource;
    private final Map<String, String> properties = new TreeMap<>();
    private final Map<String, String> applicationArguments = new TreeMap<>();

    public ApplicationConfiguration(final boolean dryRunEnabled, final char[] keystoreSecret,
            final Path keystoreSource) {
        this.dryRunEnabled = dryRunEnabled;
        this.keystoreSecret = keystoreSecret;
        this.keystoreSource = keystoreSource;
    }

    public Map<String, String> getApplicationArguments() {
        applicationArguments.put("p", String.valueOf(keystoreSecret));
        if (dryRunEnabled) {
            applicationArguments.put("d", "");
        }
        return Collections.unmodifiableMap(applicationArguments);
    }

    public Map<String, String> getJvmArguments() {
        if (dryRunEnabled) {
            return Map.ofEntries(
                    Map.entry("Xmx128m", ""),
                    Map.entry("XX:StartFlightRecording", "disk=true,dumponexit=true,maxage=1d,path-to-gc-roots=true"));
        } else {
            return Collections.singletonMap("Xmx64m", "");
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public void accept(Path distributionRoot, Path installationRoot) {
        final Path log4j2config = distributionRoot.resolve("log4j2.xml");
        if (log4j2config.toFile()
            .exists()) {
            Path log4jConfigTarget = installationRoot.resolve("log4j2.xml");
            try {
                Util.copy(log4j2config, log4jConfigTarget);
                properties.put("log4j.configurationFile", log4jConfigTarget.toString());
            } catch (final IOException ex) {
                throw new IllegalStateException("Failed creating logging configuration: " + log4jConfigTarget, ex);
            }
        }
        Path keystoreTarget = installationRoot.resolve("robozonky.keystore")
            .toAbsolutePath();
        try {
            Util.copy(keystoreSource, keystoreTarget);
            applicationArguments.put("g", keystoreTarget.toString());
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed creating keystore: " + keystoreTarget, ex);
        }
    }
}
