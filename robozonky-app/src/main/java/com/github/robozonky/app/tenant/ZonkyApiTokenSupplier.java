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

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.internal.async.Reloadable;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.secrets.SecretProvider;
import com.github.robozonky.internal.test.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Will keep permanent user authentication running in the background.
 */
class ZonkyApiTokenSupplier implements Supplier<ZonkyApiToken>,
        Closeable {

    private static final Logger LOGGER = LogManager.getLogger(ZonkyApiTokenSupplier.class);
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private final SecretProvider secrets;
    private final ApiProvider apis;
    private final Reloadable<ZonkyApiToken> token;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public ZonkyApiTokenSupplier(final ApiProvider apis, final SecretProvider secrets) {
        this.apis = apis;
        this.secrets = secrets;
        this.token = Reloadable.with(this::refreshOrFail)
                .reloadAfter(ZonkyApiTokenSupplier::reloadAfter)
                .finishWith(secrets::setToken)
                .build();
    }

    static Duration reloadAfter(final ZonkyApiToken token) {
        var now = DateUtil.offsetNow();
        var expiresOn = token.getExpiresOn();
        var halfLife = Duration.between(now, expiresOn).abs().dividedBy(2);
        if (halfLife.compareTo(ONE_HOUR) > 0) {
            return ONE_HOUR;
        } else {
            return halfLife;
        }
    }

    private static NotAuthorizedException createException(final String message) {
        var response = new ResponseBuilderImpl()
                .status(401, message)
                .build();
        return new NotAuthorizedException(response);
    }

    private static RuntimeException createException(final Throwable throwable) {
        if (throwable instanceof NotAuthorizedException || throwable instanceof BadRequestException) {
            return (RuntimeException) throwable;
        } else { // we have a problem, but that problem is not HTTP 40x, indicating auth failure
            return new IllegalStateException("Recoverable authentication failure.", throwable);
        }
    }

    private ZonkyApiToken refreshOrFail() {
        return secrets.getToken()
                .map(this::refreshOrFail)
                .orElseThrow(() -> createException("No token found."));
    }

    private ZonkyApiToken refreshOrFail(final ZonkyApiToken token) {
        if (token.isExpired()) {
            secrets.setToken(null);
            throw createException("Token expired.");
        }
        LOGGER.debug("Current token {} expiring on {}.", token, token.getExpiresOn());
        final ZonkyApiToken newToken = apis.oauth(oauth -> oauth.refresh(token));
        secrets.setToken(newToken);
        LOGGER.info("Refreshed access token for '{}', will expire on {}.", secrets.getUsername(), newToken.getExpiresOn());
        return newToken;
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
