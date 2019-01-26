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

package com.github.robozonky.app.tenant;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.api.remote.enums.OAuthScope;
import com.github.robozonky.common.async.Reloadable;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Will keep permanent user authentication running in the background.
 */
class ZonkyApiTokenSupplier implements Supplier<ZonkyApiToken>,
                                       Closeable {

    private static final Logger LOGGER = LogManager.getLogger(ZonkyApiTokenSupplier.class);
    private final OAuthScope scope;
    private final SecretProvider secrets;
    private final ApiProvider apis;
    private final Reloadable<ZonkyApiToken> token;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    ZonkyApiTokenSupplier(final ApiProvider apis, final SecretProvider secrets) {
        this(OAuthScope.SCOPE_APP_WEB, apis, secrets);
    }

    public ZonkyApiTokenSupplier(final OAuthScope scope, final ApiProvider apis, final SecretProvider secrets) {
        this.scope = scope;
        this.apis = apis;
        this.secrets = secrets;
        this.token = Reloadable.with(this::login)
                .reloadWith(this::refreshOrLogin)
                .reloadAfter(ZonkyApiTokenSupplier::reloadAfter)
                .build();
    }

    static Duration reloadAfter(final ZonkyApiToken token) {
        final int expirationInSeconds = token.getExpiresIn();
        final int minimumSecondsBeforeExpiration = 5;
        final int secondsToReloadAfter =
                Math.max(minimumSecondsBeforeExpiration, expirationInSeconds - minimumSecondsBeforeExpiration);
        return Duration.ofSeconds(secondsToReloadAfter);
    }

    private ZonkyApiToken login() {
        return apis.oauth(oauth -> {
            final String username = secrets.getUsername();
            LOGGER.info("Authenticating as '{}', requesting scope '{}'.", username, scope);
            return oauth.login(scope, username, secrets.getPassword());
        });
    }

    private ZonkyApiToken refresh(final ZonkyApiToken token) {
        LOGGER.info("Refreshing access token for '{}', scope '{}'.", secrets.getUsername(), scope);
        return apis.oauth(oauth -> oauth.refresh(token));
    }

    private ZonkyApiToken actuallyRefreshOrLogin(final ZonkyApiToken token) {
        if (token.isExpired()) {
            LOGGER.debug("Found expired token #{}.", token.getId());
            return login();
        }
        LOGGER.debug("Current token #{} expiring on {}.", token.getId(), token.getExpiresOn());
        try {
            return refresh(token);
        } catch (final Exception ex) {
            LOGGER.debug("Failed refreshing access token, falling back to password.", ex);
            return login();
        }
    }

    private ZonkyApiToken refreshOrLogin(final ZonkyApiToken token) {
        final ZonkyApiToken result = actuallyRefreshOrLogin(token);
        LOGGER.debug("Token changed from {} to {}.", token, result);
        return result;
    }

    public boolean isClosed() {
        return isClosed.get();
    }

    @Override
    public ZonkyApiToken get() {
        if (isClosed.get()) {
            throw new IllegalStateException("Token already closed.");
        }
        return token.get().getOrElseThrow(t -> new IllegalStateException("Token retrieval failed.", t));
    }

    @Override
    public void close() {
        isClosed.set(true);
        if (!token.hasValue()) {
            LOGGER.debug("Nothing to close.");
            return;
        }
        final ZonkyApiToken toClose = token.get().getOrElse(() -> null);
        if (toClose == null || toClose.isExpired()) {
            LOGGER.debug("Nothing to close or expired.");
            return;
        }
        LOGGER.info("Logging '{}' out of scope '{}'.", secrets.getUsername(), scope);
        apis.run(Zonky::logout, () -> toClose);
    }
}
