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

package com.github.triceo.robozonky.app.authentication;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.common.remote.OAuth;
import com.github.triceo.robozonky.common.secrets.SecretProvider;

class RefreshableZonkyApiToken extends Refreshable<ZonkyApiToken> {

    private static final TemporalAmount SAFETY_PRE_EXPIRATION_INTERVAL = Duration.ofSeconds(5);

    private final SecretProvider secrets;
    private final ApiProvider apis;

    public RefreshableZonkyApiToken(final ApiProvider apis, final SecretProvider secrets) {
        this.apis = apis;
        this.secrets = secrets;
    }

    private ZonkyApiToken withToken(final ZonkyApiToken token) {
        LOGGER.info("Authenticating as '{}', refreshing access token.", secrets.getUsername());
        try (final OAuth oauth = apis.oauth()) {
            return oauth.refresh(token);
        }
    }

    private ZonkyApiToken withPassword() {
        try (final OAuth oauth = apis.oauth()) {
            LOGGER.info("Authenticating as '{}', using password.", secrets.getUsername());
            return oauth.login(secrets.getUsername(), secrets.getPassword());
        }
    }

    @Override
    protected Supplier<Optional<String>> getLatestSource() {
        return () -> Optional.of(UUID.randomUUID().toString()); // refresh every time it is scheduled
    }

    @Override
    protected Optional<ZonkyApiToken> transform(final String source) {
        try {
            final ZonkyApiToken newToken = this.getLatest().map(token -> {
                if (token.willExpireIn(RefreshableZonkyApiToken.SAFETY_PRE_EXPIRATION_INTERVAL)) {
                    // may not be enough time for token refresh; rather disregard than risk auth exception
                    LOGGER.debug("Token expired or expiring too soon, using password.");
                    return withPassword();
                } else {
                    return withToken(token);
                }
            }).orElseGet(this::withPassword);
            return Optional.of(newToken);
        } catch (final Exception ex) {
            LOGGER.warn("Authentication failed.", ex);
            return Optional.empty();
        }
    }
}
