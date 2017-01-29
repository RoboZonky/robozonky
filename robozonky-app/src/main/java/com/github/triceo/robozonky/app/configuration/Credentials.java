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

package com.github.triceo.robozonky.app.configuration;

import java.util.Arrays;
import java.util.Optional;

public final class Credentials {

    private final String toolId;
    private final char[] token;

    Credentials(final String request) {
        final String[] parts = request.split(":");
        if (parts.length == 1) {
            this.toolId = parts[0];
            this.token = null;
        } else if (parts.length == 2) {
            this.toolId = parts[0];
            this.token = parts[1].toCharArray();
        } else {
            throw new IllegalArgumentException("Request must be 1 or 2 parts: " + Arrays.toString(parts));
        }
    }

    public String getToolId() {
        return toolId;
    }

    public Optional<char[]> getToken() {
        return Optional.ofNullable(this.token);
    }

    @Override
    public String toString() {
        return "Credentials{" +
                "toolId='" + toolId + '\'' +
                '}';
    }
}
