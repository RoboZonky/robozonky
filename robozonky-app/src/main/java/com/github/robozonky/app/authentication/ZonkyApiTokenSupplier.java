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

import java.io.Closeable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.ws.rs.NotAuthorizedException;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.ZonkyScope;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will keep permanent user authentication running in the background.
 */
final class ZonkyApiTokenSupplier implements Supplier<ZonkyApiToken>,
                                             Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZonkyApiTokenSupplier.class);
    private final String scope;
    private final SecretProvider secrets;
    private final ApiProvider apis;
    private final Duration refresh;
    private final AtomicReference<Boolean> isUpdating = new AtomicReference<>(false);
    private final AtomicReference<ZonkyApiToken> token = new AtomicReference<>();

    ZonkyApiTokenSupplier(final ApiProvider apis, final SecretProvider secrets, final Duration refreshAfter) {
        this(ZonkyScope.APP, apis, secrets, refreshAfter);
    }

    public ZonkyApiTokenSupplier(final ZonkyScope scope, final ApiProvider apis, final SecretProvider secrets,
                                 final Duration refreshAfter) {
        this.scope = scope.getId();
        this.apis = apis;
        this.secrets = secrets;
        // fit refresh interval between 1 and 4 minutes
        final long refreshSeconds = Math.min(240, Math.max(60, refreshAfter.get(ChronoUnit.SECONDS) - 60));
        LOGGER.debug("Token refresh may be attempted any time past {} seconds before expiration.", refreshSeconds);
        refresh = Duration.ofSeconds(refreshSeconds);
    }

    private ZonkyApiToken login() {
        return apis.oauth(oauth -> {
            final String username = secrets.getUsername();
            LOGGER.trace("Requesting '{}' as '{}', using password.", scope, username);
            return oauth.login(scope, username, secrets.getPassword());
        });
    }

    private ZonkyApiToken refreshToken(final ZonkyApiToken token) {
        LOGGER.info("Authenticating as '{}', refreshing access token.", secrets.getUsername());
        return apis.oauth(oauth -> oauth.refresh(token));
    }

    private ZonkyApiToken refreshTokenIfNecessary(final ZonkyApiToken token) {
        if (!token.willExpireIn(refresh)) {
            return token;
        }
        LOGGER.debug("Token refresh commencing.");
        isUpdating.set(true);
        try {
            return refreshToken(token);
        } catch (final Exception ex) {
            LOGGER.debug("Failed refreshing access token, falling back to password.", ex);
            return login();
        } finally {
            isUpdating.set(false);
            LOGGER.debug("Token refresh over.");
        }
    }

    private synchronized ZonkyApiToken getTokenInAnyWay(final ZonkyApiToken currentToken) {
        return currentToken == null ? login() : refreshTokenIfNecessary(currentToken);
    }

    public boolean isAvailable() {
        return !(token.get() == null || isUpdating.get());
    }

    /*
     * Synchronized so that the operation on the token is always only happening once and multiple threads therefore
     * cannot cancel out each others' token requests.
     */
    @Override
    public ZonkyApiToken get() {
        try {
            return token.updateAndGet(this::getTokenInAnyWay);
        } catch (final Exception ex) {
            throw new NotAuthorizedException(ex);
        }
    }

    @Override
    public synchronized void close() {
        final ZonkyApiToken beforeClosing = token.getAndSet(null);
        if (beforeClosing == null || beforeClosing.willExpireIn(Duration.ZERO)) {
            return;
        }
        LOGGER.debug("Logging '{}' out of Zonky ({}).", secrets.getUsername(), scope);
        apis.run(Zonky::logout, () -> beforeClosing);
    }
}
