/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.app.tenant;

import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.secrets.SecretProvider;

public final class TenantBuilder {

    private String name = null;
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

    public PowerTenant build(boolean enableDryRun) {
        if (secrets == null) {
            throw new IllegalStateException("Secret provider must be provided.");
        }
        var apiProvider = api == null ? new ApiProvider(name) : api;
        var tokenSupplier = new ZonkyApiTokenSupplier(apiProvider, secrets);
        return new PowerTenantImpl(secrets.getUsername(), name, enableDryRun, apiProvider, strategyProvider,
                tokenSupplier);
    }

}
