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

package com.github.robozonky.app.authentication;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.remote.ZonkyApiTokenSupplier;
import com.github.robozonky.common.secrets.SecretProvider;

class TokenBasedTenant implements Tenant {

    private final Supplier<ZonkyApiToken> tokenSupplier;
    private final SessionInfo sessionInfo;
    private final ApiProvider apis;
    private final SecretProvider secrets;

    private Instant lastRestrictionsUpdate = Instant.EPOCH;
    private Restrictions restrictions = null;

    TokenBasedTenant(final ApiProvider apis, final SecretProvider secrets, final String sessionName,
                     final boolean isDryRun, final Duration refreshAfter) {
        this.secrets = secrets;
        this.apis = apis;
        this.sessionInfo = new SessionInfo(secrets.getUsername(), sessionName, isDryRun);
        this.tokenSupplier = new ZonkyApiTokenSupplier(apis, secrets, refreshAfter);
    }

    @Override
    public Restrictions getRestrictions() {
        return getRestrictions(Instant.now());
    }

    synchronized Restrictions getRestrictions(final Instant now) {
        final boolean needsUpdate = lastRestrictionsUpdate.plus(Duration.ofMinutes(5)).isBefore(now);
        if (needsUpdate) {
            restrictions = call(Zonky::getRestrictions);
            lastRestrictionsUpdate = now;
        }
        return restrictions;
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation) {
        return apis.call(operation, tokenSupplier);
    }

    @Override
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    @Override
    public SecretProvider getSecrets() {
        return secrets;
    }
}
