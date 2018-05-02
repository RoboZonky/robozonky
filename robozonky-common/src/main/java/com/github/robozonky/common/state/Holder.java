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
import java.util.Objects;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.internal.api.Settings;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

final class Holder {

    private static final File TARGET = Settings.INSTANCE.getStateFile();
    private static final StateStorage PARENT = new FileBackedStateStorage(TARGET);
    private static volatile MutableMap<String, TenantState> TENANT_STATE_MAP = UnifiedMap.newMap(0);

    private Holder() {
        // no instances
    }

    private static String identify(final SessionInfo sessionInfo) {
        return sessionInfo.getUsername();
    }

    public static TenantState of(final SessionInfo session) {
        return of(session, PARENT);
    }

    static synchronized TenantState of(final SessionInfo session, final StateStorage underlying) {
        return TENANT_STATE_MAP.computeIfAbsent(identify(session),
                                                tenantName -> new TenantState(tenantName, underlying));
    }

    static synchronized void destroy(final TenantState tenantState) {
        TENANT_STATE_MAP = TENANT_STATE_MAP.reject((id, current) -> Objects.equals(current, tenantState));
    }

    public static synchronized void destroy() {
        TENANT_STATE_MAP.forEach((id, state) -> state.destroy());
        TENANT_STATE_MAP = UnifiedMap.newMap(0);
        TARGET.delete();
    }
}
