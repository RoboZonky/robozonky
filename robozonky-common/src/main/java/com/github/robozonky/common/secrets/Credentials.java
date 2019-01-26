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

package com.github.robozonky.common.secrets;

import java.util.Arrays;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Credentials {

    private static final Logger LOGGER = LogManager.getLogger(Credentials.class);

    private final String toolId;
    private final SecretProvider secretProvider;

    public Credentials(final String request, final SecretProvider secretProvider) {
        this.secretProvider = secretProvider;
        final String[] parts = request.split(":");
        if (parts.length == 1) {
            this.toolId = parts[0];
            LOGGER.debug("Credentials for '{}' not given password.", this.toolId);
        } else if (parts.length == 2) {
            this.toolId = parts[0];
            this.secretProvider.setSecret(this.toolId, parts[1].toCharArray());
            LOGGER.debug("Credentials for '{}' stored password.", this.toolId);
        } else {
            throw new IllegalArgumentException("Request must be 1 or 2 parts: " + Arrays.toString(parts));
        }
    }

    public String getToolId() {
        return toolId;
    }

    public Optional<char[]> getToken() {
        return secretProvider.getSecret(this.toolId);
    }

    @Override
    public String toString() {
        return "Credentials{" +
                "toolId='" + toolId + '\'' +
                '}';
    }
}
