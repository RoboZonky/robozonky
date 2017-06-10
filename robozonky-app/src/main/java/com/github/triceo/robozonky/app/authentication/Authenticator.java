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

import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.common.remote.Api;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to authenticate to the Zonky API. Use either {@link #withAccessToken(String, ZonkyApiToken, TemporalAmount)},
 * or {@link #withCredentials(String, char[])} to log in.
 */
abstract class Authenticator implements Function<ApiProvider, ZonkyApiToken> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Authenticator.class);
    private static final String TARGET_SCOPE = "SCOPE_APP_WEB";

    /**
     * Prepare for authentication using username and password.
     *
     * @param username Zonky username.
     * @param password Zonky password.
     * @return Instance ready for authentication.
     */
    public static Function<ApiProvider, ZonkyApiToken> withCredentials(final String username, final char... password) {
        return new Authenticator() {
            @Override
            protected ZonkyApiToken getAuthenticationMethod(final ZonkyOAuthApi api) {
                Authenticator.LOGGER.info("Authenticating as '{}' using password.", username);
                return api.login(username, new String(password), "password", Authenticator.TARGET_SCOPE);
            }
        };
    }

    /**
     * Prepare for authentication using the Zonky OAuth token which will also refresh the token.
     *
     * @param username Zonky username.
     * @param token OAuth token.
     * @param refreshBeforeExpiration How long before token expiration to refresh the token.
     * @return Instance ready for authentication, or empty if token still fresh.
     */
    public static Function<ApiProvider, ZonkyApiToken> withAccessToken(final String username,
                                                                       final ZonkyApiToken token,
                                                                       final TemporalAmount refreshBeforeExpiration) {
        if (token.willExpireIn(refreshBeforeExpiration)) {
            return new Authenticator() {
                @Override
                protected ZonkyApiToken getAuthenticationMethod(final ZonkyOAuthApi api) {
                    Authenticator.LOGGER.info("Authenticating as '{}', refreshing existing access token.", username);
                    final String tokenId = String.valueOf(token.getRefreshToken());
                    return api.refresh(tokenId, "refresh_token", Authenticator.TARGET_SCOPE);
                }
            };
        } else { // auth token is still up to date; don't even create the API endpoint
            return (api) -> token;
        }
    }

    protected abstract ZonkyApiToken getAuthenticationMethod(final ZonkyOAuthApi api);

    /**
     * Perform the actual authentication. Will throw an unchecked exception in case authentication failed.
     * @param provider The provider to be used when constructing the APIs.
     * @return Information about the authentication.
     */
    @Override
    public ZonkyApiToken apply(final ApiProvider provider) {
        try (final Api<ZonkyOAuthApi> api = provider.oauth()) {
            return api.execute(this::getAuthenticationMethod);
        } catch (final BadRequestException ex) {
            throw new WebApplicationException("Failed authenticating with Zonky, check your password.", ex);
        }
    }

}
