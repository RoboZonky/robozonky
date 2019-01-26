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

package com.github.robozonky.common.state;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.internal.api.Defaults;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TenantState {

    private static final Logger LOGGER = LogManager.getLogger(TenantState.class);
    private static final Map<SessionInfo, TenantState> TENANT_STATE_MAP = new ConcurrentHashMap<>(0);
    private final FileBackedStateStorage stateStorage;

    TenantState(final SessionInfo sessionInfo) { // no external instances
        this.stateStorage = new FileBackedStateStorage(getFile(sessionInfo.getUsername()));
        LOGGER.debug("Created new tenant state for {}: {}.", sessionInfo, this);
    }

    public static TenantState of(final SessionInfo session) {
        return TENANT_STATE_MAP.computeIfAbsent(session, TenantState::new);
    }

    public static Stream<SessionInfo> getKnownTenants() {
        return TENANT_STATE_MAP.keySet().stream();
    }

    static String encode(final String secret) {
        return Try.of(() -> {
            final MessageDigest mdEnc = MessageDigest.getInstance("MD5");
            mdEnc.update(secret.getBytes(Defaults.CHARSET));
            return new BigInteger(1, mdEnc.digest()).toString(16);
        }).getOrElse(secret);
    }

    private static File getFile(final String username) {
        final String encoded = encode(username);
        final String filename = "robozonky-" + encoded + ".state";
        return new File(filename);
    }

    /**
     * For testing purposes only.
     */
    public static void destroyAll() {
        getKnownTenants().map(TenantState::of).forEach(t -> {
            LOGGER.debug("Destroying state for {}.", t);
            t.stateStorage.destroy();
        });
        TENANT_STATE_MAP.clear();
    }

    StateStorage getStateStorage() {
        return stateStorage;
    }

    public <T> InstanceState<T> in(final Class<T> cls) {
        return new InstanceStateImpl<>(this, cls.getName(), stateStorage);
    }
}
