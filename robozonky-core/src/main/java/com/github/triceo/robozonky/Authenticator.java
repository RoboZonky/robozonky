/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky;

import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to authenticate to the Zonky API. Use either {@link #withAccessToken(String, ZonkyApiToken)},
 * {@link #withAccessTokenAndRefresh(String, ZonkyApiToken)} or
 * {@link #withCredentials(String, char[])} to log in.
 */
public class Authenticator {

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
            final ZonkyApiToken token =
                    api.login(username, new String(password), "password", Authenticator.TARGET_SCOPE);
            Authenticator.LOGGER.info("Logged in with Zonky as user '{}' using password.", username);
            return token;
        }, false);
    }

    /**
     * Prepare for authentication using the Zonky OAuth token.
     *
     * @param username Zonky username.
     * @param token OAuth token.
     * @return Instance ready for authentication.
     */
    public static Authenticator withAccessToken(final String username, final ZonkyApiToken token) {
        return new Authenticator(api -> {
            Authenticator.LOGGER.info("Logged in with Zonky as user '{}' with existing access token.", username);
            return token;
        }, true);
    }

    /**
     * Prepare for authentication using the Zonky OAuth token which will also refresh the token.
     *
     * @param username Zonky username.
     * @param token OAuth token.
     * @return Instance ready for authentication.
     */
    public static Authenticator withAccessTokenAndRefresh(final String username, final ZonkyApiToken token) {
        return new Authenticator(api -> {
            final String tokenId = String.valueOf(token.getRefreshToken());
            final ZonkyApiToken newToken = api.refresh(tokenId, "refresh_token", Authenticator.TARGET_SCOPE);
            Authenticator.LOGGER.info("Logged in with Zonky as user '{}', refreshing existing access token.", username);
            return newToken;
        }, true);
    }

    private final Function<ZonkyOAuthApi, ZonkyApiToken> authenticationMethod;
    private final boolean tokenBased;

    private Authenticator(final Function<ZonkyOAuthApi, ZonkyApiToken> authenticationMethod, final boolean tokenBased) {
        if (authenticationMethod == null) {
            throw new IllegalArgumentException("Authentication method must be provided.");
        }
        this.authenticationMethod = authenticationMethod;
        this.tokenBased = tokenBased;
    }

    /**
     * Whether or not this particular authentication will use the Zonky OAuth access token.
     * @return True if token is being used.
     */
    public boolean isTokenBased() {
        return tokenBased;
    }

    /**
     * Perform the actual authentication. Will throw an unchecked exception in case authentication failed.
     * @param provider The provider to be used when constructing the APIs.
     * @return Information about the authentication.
     */
    public Authentication authenticate(final ApiProvider provider) {
        final ZonkyOAuthApi api = provider.oauth();
        final ZonkyApiToken token = authenticationMethod.apply(api);
        final ZonkyApi result = provider.authenticated(token);
        return new Authentication(result, token);
    }

}
