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
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.ZonkyScope;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.internal.api.Settings;

public final class TenantBuilder {

    private String name = null;
    private boolean dryRun = false;
    private SecretProvider secrets = null;
    private StrategyProvider strategyProvider = StrategyProvider.empty();
    private ApiProvider api;

    public TenantBuilder withSecrets(final SecretProvider secrets) {
        this.secrets = secrets;
        return this;
    }

    public TenantBuilder withStrategy(final String strategyLocation) {
        this.strategyProvider = StrategyProvider.createFor(strategyLocation);
        return this;
    }

    public TenantBuilder named(final String name) {
        this.name = name;
        return this;
    }

    public TenantBuilder withApi(final ApiProvider api) {
        this.api = api;
        return this;
    }

    public TenantBuilder dryRun() {
        this.dryRun = true;
        return this;
    }

    public Tenant build(final Duration tokenRefresh) {
        if (secrets == null) {
            throw new IllegalStateException("Secret provider must be provided.");
        }
        final ApiProvider apis = api == null ? new ApiProvider() : api;
        final Function<ZonkyScope, ZonkyApiTokenSupplier> tokenSupplier =
                scope -> new ZonkyApiTokenSupplier(scope, apis, secrets, tokenRefresh);
        final SessionInfo sessionInfo = new SessionInfo(secrets.getUsername(), name, dryRun);
        return new TokenBasedTenant(sessionInfo, apis, strategyProvider, tokenSupplier);
    }

    public Tenant build() {
        return build(Settings.INSTANCE.getTokenRefreshPeriod());
    }
}


