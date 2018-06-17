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

package com.github.robozonky.common.state;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.internal.api.Settings;

final class Holder {

    private static final File TARGET = Settings.INSTANCE.getStateFile();
    private static volatile Map<String, TenantState> TENANT_STATE_MAP = new HashMap<>(0);

    private Holder() {
        // no instances
    }

    private static String identify(final SessionInfo sessionInfo) {
        return sessionInfo.getUsername();
    }

    public static synchronized TenantState of(final SessionInfo session) {
        return TENANT_STATE_MAP.computeIfAbsent(identify(session), TenantState::new);
    }

    public static synchronized Collection<String> getKnownTenants() {
        return Collections.unmodifiableSet(new HashSet<>(TENANT_STATE_MAP.keySet()));
    }

    static synchronized void destroy(final TenantState tenantState) {
        TENANT_STATE_MAP = TENANT_STATE_MAP.entrySet().stream()
                .filter(e -> !Objects.equals(e.getValue(), tenantState))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static synchronized void destroy() {
        TENANT_STATE_MAP.forEach((id, state) -> state.destroy());
        TENANT_STATE_MAP = new HashMap<>(0);
        TARGET.delete();
    }
}
