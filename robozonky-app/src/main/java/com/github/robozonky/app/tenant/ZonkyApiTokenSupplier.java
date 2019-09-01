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
import javax.ws.rs.NotAuthorizedException;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.api.remote.enums.OAuthScope;
import com.github.robozonky.internal.async.Reloadable;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.secrets.SecretProvider;
import com.github.robozonky.internal.test.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;

/**
 * Will keep permanent user authentication running in the background.
 */
class ZonkyApiTokenSupplier implements Supplier<ZonkyApiToken>,
                                       Closeable {

    private static final Logger LOGGER = LogManager.getLogger(ZonkyApiTokenSupplier.class);
    private static final Duration ONE_HOUR = Duration.ofHours(1);

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
        this.token = Reloadable.with(this::refreshOrLogin)
                .reloadAfter(ZonkyApiTokenSupplier::reloadAfter)
                .finishWith(secrets::setToken)
                .build();
    }

    static Duration reloadAfter(final ZonkyApiToken token) {
        var expiresOn = token.getExpiresOn();
        var now = DateUtil.offsetNow();
        var untilExpiration = Duration.between(expiresOn, now).abs();
        if (untilExpiration.compareTo(ONE_HOUR) > 0) {
            return ONE_HOUR;
        } else {
            return untilExpiration.dividedBy(10);
        }
    }

    private static NotAuthorizedException createException(final String message) {
        var response = new ResponseBuilderImpl()
                .status(401, message)
                .build();
        return new NotAuthorizedException(response);
    }

    private static NotAuthorizedException createException(final Throwable throwable) {
        var response = new ResponseBuilderImpl()
                .status(401)
                .build();
        return new NotAuthorizedException(response, throwable);
    }

    private ZonkyApiToken refreshOrLogin() {
        return secrets.getToken()
                .map(this::refreshOrLogin)
                .orElseGet(() -> apis.oauth(oauth -> {
                               final String username = secrets.getUsername();
                               LOGGER.info("Authenticating as '{}', requesting scope '{}'.", username, scope);
                               return oauth.login(scope, username, secrets.getPassword());
                           })
                );
    }

    private ZonkyApiToken refreshOrLogin(final ZonkyApiToken token) {
        if (token.isExpired()) {
            LOGGER.debug("Found expired token #{}.", token.getId());
            secrets.setToken(null);
            return refreshOrLogin();
        }
        LOGGER.debug("Current token #{} expiring on {}.", token.getId(), token.getExpiresOn());
        LOGGER.info("Refreshing access token for '{}', scope '{}'.", secrets.getUsername(), scope);
        return apis.oauth(oauth -> oauth.refresh(token));
    }

    public boolean isClosed() {
        return isClosed.get();
    }

    @Override
    public ZonkyApiToken get() {
        if (isClosed.get()) {
            throw createException("Token already closed.");
        }
        return token.get().getOrElseThrow(ZonkyApiTokenSupplier::createException);
    }

    @Override
    public void close() {
        isClosed.set(true);
        LOGGER.debug("Token closed.");
    }
}
