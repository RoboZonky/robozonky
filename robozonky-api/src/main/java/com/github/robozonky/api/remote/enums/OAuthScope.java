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

package com.github.robozonky.api.remote.enums;

import java.util.Objects;
import java.util.stream.Stream;

public enum OAuthScope {

    SCOPE_APP_OAUTH,
    SCOPE_APP_WEB,
    SCOPE_FILE_DOWNLOAD;

    public static OAuthScope findByCode(final String code) {
        return Stream.of(OAuthScope.values())
                .filter(r -> Objects.equals(r.name(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown OAuth scope: " + code));
    }

}
