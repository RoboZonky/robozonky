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

package com.github.robozonky.cli.configuration;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Map;

public interface PropertyConfiguration extends Configuration {

    static PropertyConfiguration disabledJmx() {
        return new DisabledJmxConfiguration();
    }

    static PropertyConfiguration enabledJmx(final String jmxHostname, final int jmxPort,
            final boolean jmxSecurityEnabled) {
        return new EnabledJmxConfiguration(requireNonNull(jmxHostname), jmxPort, jmxSecurityEnabled);
    }

    static PropertyConfiguration applicationDryRun(final Path keystoreSource, final char... keystoreSecret) {
        return new ApplicationConfiguration(true, keystoreSecret, keystoreSource);
    }

    static PropertyConfiguration applicationReal(final Path keystoreSource, final char... keystoreSecret) {
        return new ApplicationConfiguration(false, keystoreSecret, keystoreSource);
    }

    Map<String, String> getProperties();

}
