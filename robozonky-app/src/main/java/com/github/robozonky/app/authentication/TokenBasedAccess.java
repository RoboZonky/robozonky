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
import java.time.temporal.TemporalAmount;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.ServiceUnavailableException;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TokenBasedAccess implements Authenticated {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBasedAccess.class);
    private static final TemporalAmount EMERGENCY_REFRESH_INTERVAL = Duration.ofSeconds(5);
    private final Lock exclusiveRefresh = new ReentrantLock(true);
    private final Refreshable<ZonkyApiToken> refreshableToken;
    private final SecretProvider secrets;
    private final ApiProvider apis;

    TokenBasedAccess(final ApiProvider apis, final SecretProvider secrets, final TemporalAmount refreshAfter) {
        this.apis = apis;
        this.secrets = secrets;
        this.refreshableToken = new RefreshableZonkyApiToken(apis, secrets);
        final long refreshSeconds = Math.max(60, refreshAfter.get(ChronoUnit.SECONDS) - 60);
        final TemporalAmount refresh = Duration.ofSeconds(refreshSeconds);
        Scheduler.inBackground().submit(refreshableToken, refresh);
    }

    Refreshable<ZonkyApiToken> getRefreshableToken() {
        return refreshableToken;
    }

    private ZonkyApiToken getToken() {
        return refreshableToken.getLatest()
                .orElseThrow(() -> new ServiceUnavailableException("No API token available, authentication failed."));
    }

    private ZonkyApiToken getFreshToken() {
        exclusiveRefresh.lock();
        try {
            // no point in making multiple threads refresh the same token
            final ZonkyApiToken token = getToken();
            if (token.willExpireIn(EMERGENCY_REFRESH_INTERVAL)) {
                LOGGER.debug("Emergency refresh to prevent token from going stale on {}.", token.getExpiresOn());
                refreshableToken.run();
                return getToken();
            } else {
                return token;
            }
        } finally {
            exclusiveRefresh.unlock();
        }
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation) {
        return call(operation, 1);
    }

    public <T> T call(final Function<Zonky, T> operation, final int attemptNo) {
        LOGGER.trace("Executing {}, attempt #{}.", operation, attemptNo);
        try {
            final T result = apis.authenticated(getFreshToken(), operation);
            LOGGER.trace("Done with {}, attempt #{}.", operation, attemptNo);
            return result;
        } catch (final NotAuthorizedException ex) {
            if (attemptNo >= 3) {
                throw new IllegalStateException("There was a severe authorization problem.", ex);
            } else {
                LOGGER.debug("Request failed due to expired token, will retry #" + attemptNo);
                return call(operation, attemptNo + 1);
            }
        }
    }

    @Override
    public SecretProvider getSecretProvider() {
        return secrets;
    }
}
