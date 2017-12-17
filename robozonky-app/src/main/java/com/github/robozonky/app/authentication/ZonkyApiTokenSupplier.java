/*
 * Copyright 2017 The RoboZonky Project
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
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.ws.rs.BadRequestException;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.secrets.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will keep permanent user authentication running in the background.
 */
class ZonkyApiTokenSupplier implements Supplier<Optional<ZonkyApiToken>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZonkyApiTokenSupplier.class);

    private final SecretProvider secrets;
    private final ApiProvider apis;
    private final AtomicReference<ZonkyApiToken> token = new AtomicReference<>();
    private final Duration refresh;

    public ZonkyApiTokenSupplier(final ApiProvider apis, final SecretProvider secrets, final Duration refreshAfter) {
        this.apis = apis;
        this.secrets = secrets;
        // fit refresh interval between 1 and 4 minutes
        final long refreshSeconds = Math.min(240, Math.max(60, refreshAfter.get(ChronoUnit.SECONDS) - 60));
        LOGGER.debug("Token refresh may be attempted any time past {} seconds before expiration.", refreshSeconds);
        refresh = Duration.ofSeconds(refreshSeconds);
    }

    private ZonkyApiToken refreshToken(final ZonkyApiToken token) {
        LOGGER.info("Authenticating as '{}', refreshing access token.", secrets.getUsername());
        try {
            return apis.oauth((oauth) -> oauth.refresh(token));
        } catch (final BadRequestException ex) { // possibly just an expired token, retry with password
            LOGGER.debug("Failed refreshing access token, using password.");
            return getFreshToken();
        }
    }

    private ZonkyApiToken getFreshToken() {
        return PasswordBasedAccess.trigger(apis, secrets.getUsername(), secrets.getPassword());
    }

    private ZonkyApiToken refreshTokenIfNecessary(final ZonkyApiToken token) {
        if (token.willExpireIn(refresh)) {
            return refreshToken(token);
        } else {
            LOGGER.trace("Reusing token.");
            return token;
        }
    }

    /*
     * Synchronized so that the operation on the token is always only happening once and multiple threads therefore
     * cannot cancel out each others' token requests.
     */
    private synchronized ZonkyApiToken getTokenInAnyWay(final ZonkyApiToken currentToken) {
        return currentToken == null ? getFreshToken() : refreshTokenIfNecessary(currentToken);
    }

    @Override
    public Optional<ZonkyApiToken> get() {
        try {
            final ZonkyApiToken newToken = token.updateAndGet(this::getTokenInAnyWay);
            return Optional.ofNullable(newToken);
        } catch (final Exception ex) {
            LOGGER.warn("Authentication failed.", ex);
            return Optional.empty();
        }
    }
}
