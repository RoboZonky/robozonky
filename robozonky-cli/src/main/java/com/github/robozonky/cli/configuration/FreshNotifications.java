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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

final class FreshNotifications implements NotificationConfiguration {

    private final Properties properties;
    private final AtomicReference<String> finalLocation = new AtomicReference<>();

    public FreshNotifications(final Properties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> getFinalLocation() {
        return Optional.of(finalLocation.get());
    }

    @Override
    public void accept(Path distributionRoot, Path installationRoot) {
        Path target = installationRoot.resolve("robozonky-notifications.cfg");
        try {
            Util.writeOutProperties(properties, target.toFile());
            finalLocation.set(target.toUri()
                .toString());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed writing notification properties: " + ex);
        }
    }
}
