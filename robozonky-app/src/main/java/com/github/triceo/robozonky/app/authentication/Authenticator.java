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

import java.time.temporal.TemporalAmount;
import java.util.function.Function;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;

import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to authenticate to the Zonky API. Use either {@link #withAccessToken(String, ZonkyApiToken, TemporalAmount)},
 * or {@link #withCredentials(String, char[])} to log in.
 */
class Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Authenticator.class);
    private static final String TARGET_SCOPE = "SCOPE_APP_WEB";

    /**
     * Prepare for authentication using username and password.
     *
     * @param username Zonky username.
     * @param password Zonky password.
     * @return Instance ready for authentication.
     */
    public static Authenticator withCredentials(final String username, final char[] password) {
        return new Authenticator(api -> {
            Authenticator.LOGGER.info("Authenticating as '{}' using password.", username);
            final ZonkyApiToken token =
                    api.login(username, new String(password), "password", Authenticator.TARGET_SCOPE);
            Authenticator.LOGGER.debug("Authenticated.");
            return token;
        });
    }

    /**
     * Prepare for authentication using the Zonky OAuth token which will also refresh the token.
     *
     * @param username Zonky username.
     * @param token OAuth token.
     * @return Instance ready for authentication.
     */
    public static Authenticator withAccessToken(final String username, final ZonkyApiToken token,
                                                final TemporalAmount tokenRefreshBeforeExpiration) {
        return new Authenticator(api -> {
            if (token.willExpireIn(tokenRefreshBeforeExpiration)) {
                final String tokenId = String.valueOf(token.getRefreshToken());
                final ZonkyApiToken newToken = api.refresh(tokenId, "refresh_token", Authenticator.TARGET_SCOPE);
                Authenticator.LOGGER.info("Authenticating as '{}', refreshing existing access token.", username);
                return newToken;
            } else {
                Authenticator.LOGGER.info("Authenticated as '{}', reusing existing access token.", username);
                return token;
            }
        });
    }

    private final Function<ZonkyOAuthApi, ZonkyApiToken> authenticationMethod;

    private Authenticator(final Function<ZonkyOAuthApi, ZonkyApiToken> authenticationMethod) {
        if (authenticationMethod == null) {
            throw new IllegalArgumentException("Authentication method must be provided.");
        }
        this.authenticationMethod = authenticationMethod;
    }

    /**
     * Perform the actual authentication. Will throw an unchecked exception in case authentication failed.
     * @param provider The provider to be used when constructing the APIs.
     * @return Information about the authentication.
     */
    public Authentication authenticate(final ApiProvider provider) {
        final ZonkyOAuthApi api = provider.oauth();
        try {
            final ZonkyApiToken token = authenticationMethod.apply(api);
            return new Authenticator.AuthenticationImpl(provider, token);
        } catch (final BadRequestException ex) {
            throw new WebApplicationException("Failed authenticating with Zonky, check your password.", ex);
        }
    }

    private static class AuthenticationImpl implements Authentication {

        private final ZonkyApi zonkyApi;
        private final ZonkyApiToken token;

        public AuthenticationImpl(final ApiProvider provider, final ZonkyApiToken token) {
            this.zonkyApi = provider.authenticated(token);
            this.token = token;
        }

        @Override
        public ZonkyApi getZonkyApi() {
            return zonkyApi;
        }

        @Override
        public ZonkyApiToken getZonkyApiToken() {
            return token;
        }

        @Override
        public boolean willExpireIn(final TemporalAmount temporalAmount) {
            return token.willExpireIn(temporalAmount);
        }

    }
}
