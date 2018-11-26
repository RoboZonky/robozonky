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
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.common.RemoteBalance;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.ZonkyScope;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.util.Reloadable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TokenBasedTenant implements Tenant {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBasedTenant.class);
    private static final Restrictions FULLY_RESTRICTED = new Restrictions();

    private final SessionInfo sessionInfo;
    private final ApiProvider apis;
    private final SecretProvider secrets;
    private final Function<ZonkyScope, ZonkyApiTokenSupplier> supplier;
    private final Map<ZonkyScope, ZonkyApiTokenSupplier> tokens = new EnumMap<>(ZonkyScope.class);
    private final RemoteBalance balance;
    private final Reloadable<Restrictions> restrictions;

    TokenBasedTenant(final ApiProvider apis, final SecretProvider secrets, final String sessionName,
                     final boolean isDryRun, final Duration refreshAfter) {
        this.secrets = secrets;
        this.apis = apis;
        this.sessionInfo = new SessionInfo(secrets.getUsername(), sessionName, isDryRun);
        this.supplier = scope -> new ZonkyApiTokenSupplier(scope, apis, secrets, refreshAfter);
        this.balance = new RemoteBalanceImpl(this);
        this.restrictions = Reloadable.of(() -> this.call(Zonky::getRestrictions), Duration.ofHours(1));
    }

    @Override
    public Restrictions getRestrictions() {
        return restrictions.get().getOrElseGet(ex -> {
            LOGGER.info("Failed retrieving Zonky restrictions, disabling all operations.", ex);
            return FULLY_RESTRICTED;
        });
    }

    private ZonkyApiTokenSupplier getTokenSupplier(final ZonkyScope scope) {
        return tokens.computeIfAbsent(scope, supplier);
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation, final ZonkyScope scope) {
        return apis.call(operation, getTokenSupplier(scope));
    }

    @Override
    public boolean isAvailable(final ZonkyScope scope) {
        return getTokenSupplier(scope).isAvailable();
    }

    @Override
    public RemoteBalance getBalance() {
        return balance;
    }

    @Override
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    @Override
    public SecretProvider getSecrets() {
        return secrets;
    }

    @Override
    public void close() { // cancel existing tokens
        tokens.forEach((k, v) -> v.close());
    }
}
