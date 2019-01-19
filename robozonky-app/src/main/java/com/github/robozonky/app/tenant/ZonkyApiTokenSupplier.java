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

package com.github.robozonky.app.tenant;

import java.io.Closeable;
import java.time.Duration;
import java.util.function.Supplier;
import javax.ws.rs.NotAuthorizedException;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.async.Reloadable;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.common.tenant.ZonkyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will keep permanent user authentication running in the background.
 */
final class ZonkyApiTokenSupplier implements Supplier<ZonkyApiToken>,
                                             Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZonkyApiTokenSupplier.class);
    private final ZonkyScope scope;
    private final SecretProvider secrets;
    private final ApiProvider apis;
    private final Reloadable<ZonkyApiToken> token;

    ZonkyApiTokenSupplier(final ApiProvider apis, final SecretProvider secrets, final Duration refreshAfter) {
        this(ZonkyScope.APP, apis, secrets, refreshAfter);
    }

    public ZonkyApiTokenSupplier(final ZonkyScope scope, final ApiProvider apis, final SecretProvider secrets,
                                 final Duration refreshAfter) {
        this.scope = scope;
        this.apis = apis;
        this.secrets = secrets;
        this.token = Reloadable.with(this::login)
                .reloadWith(this::refreshOrLogin)
                .reloadAfter(refreshAfter)
                .build();
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

    private ZonkyApiToken refreshOrLogin(final ZonkyApiToken token) {
        if (token.willExpireIn(Duration.ZERO)) {
            LOGGER.debug("Found expired token for '{}', scope '{}'.", secrets.getUsername(), scope);
            return login();
        }
        LOGGER.debug("Current token expiring on {}.", token.getExpiresOn());
        try {
            return refresh(token);
        } catch (final Exception ex) {
            LOGGER.debug("Failed refreshing access token, falling back to password.", ex);
            return login();
        }
    }

    @Override
    public ZonkyApiToken get() {
        return token.get().getOrElseThrow(t -> new NotAuthorizedException(t));
    }

    @Override
    public void close() {
        if (!token.hasValue()) {
            LOGGER.debug("Nothing to close.");
            return;
        }
        final ZonkyApiToken toClose = token.get().getOrElse(() -> null);
        if (toClose == null || toClose.willExpireIn(Duration.ZERO)) {
            LOGGER.debug("Nothing to close or expired.");
            return;
        }
        LOGGER.info("Logging '{}' out of scope '{}'.", secrets.getUsername(), scope);
        apis.run(Zonky::logout, () -> toClose);
    }
}
