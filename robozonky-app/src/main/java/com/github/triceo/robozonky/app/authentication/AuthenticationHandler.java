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

import java.io.StringReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.function.Function;
import javax.xml.bind.JAXBException;

import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.internal.api.AbstractApiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationHandler.class);

    /**
     * Build authentication mechanism that will keep the session alive via the use of session token. The mechanism will
     * never log out, but the session may expire if not refresh regularly. This is potentially unsafe, as it will
     * eventually store a plain-text access token on the hard drive, for everyone to see.
     *
     * The token will only be refreshed if RoboZonky is launched between token expiration and X second before token
     * expiration, where X comes from the arguments of this method.
     *
     * @param data Provider for the sensitive information, such as passwords and tokens.
     * @param refreshAfter Access token will be refreshed after expiration minus this.
     * @return This.
     */
    public static AuthenticationHandler tokenBased(final SecretProvider data, final TemporalAmount refreshAfter) {
        return new AuthenticationHandler(data, true, refreshAfter);
    }

    /**
     * Build authentication mechanism that will log out at the end of RoboZonky's operations. This will ignore the
     * access tokens.
     *
     * @param data Provider for the sensitive information, such as passwords and tokens.
     * @return The desired authentication method.
     */
    public static AuthenticationHandler passwordBased(final SecretProvider data) {
        return new AuthenticationHandler(data);
    }

    private final boolean tokenBased;
    private final SecretProvider data;
    private final TemporalAmount tokenRefreshBeforeExpiration;

    private AuthenticationHandler(final SecretProvider data) {
        this(data, false, Duration.ZERO);
    }

    private AuthenticationHandler(final SecretProvider data, final boolean tokenBased,
                                  final TemporalAmount tokenRefreshBeforeExpiration) {
        this.data = data;
        this.tokenRefreshBeforeExpiration = tokenRefreshBeforeExpiration;
        this.tokenBased = tokenBased;
    }

    public SecretProvider getSecretProvider() {
        return data;
    }

    private Function<ApiProvider, Authentication> buildAuthenticatorWithPassword() {
        return Authenticator.withCredentials(this.data.getUsername(), this.data.getPassword());
    }

    /**
     * Based on information received until this point, decide on the proper authentication method.
     *
     * @return Authentication method matching user preferences.
     */
    private Function<ApiProvider, Authentication> buildAuthenticator() {
        if (!this.tokenBased) {
            AuthenticationHandler.LOGGER.debug("Password-based authentication requested.");
            return this.buildAuthenticatorWithPassword();
        }
        return this.data.getToken().map(r -> {
            try {
                final long refreshIntervalInSeconds = tokenRefreshBeforeExpiration.get(ChronoUnit.SECONDS);
                final ZonkyApiToken token = ZonkyApiToken.unmarshal(r);
                AuthenticationHandler.LOGGER.debug("Token obtained: {}, expires: {}. Refresh {} seconds before.",
                        token.getObtainedOn(), token.getExpiresOn(), refreshIntervalInSeconds);
                final int safetyRefreshIntervalInSeconds = 5;
                if (token.willExpireIn(Duration.ofSeconds(safetyRefreshIntervalInSeconds))) {
                    if (safetyRefreshIntervalInSeconds > refreshIntervalInSeconds) {
                        AuthenticationHandler.LOGGER.info("Ignoring pre-set token refresh time.");
                    }
                    // may not be enough time for token refresh; rather disregard than risk auth exception
                    AuthenticationHandler.LOGGER.debug("Token expired or expiring too soon, using password.");
                    return this.buildAuthenticatorWithPassword();
                } else {
                    return Authenticator.withAccessToken(this.data.getUsername(), token, tokenRefreshBeforeExpiration);
                }
            } catch (final Exception ex) {
                AuthenticationHandler.LOGGER.warn("Failed parsing token, using password.", ex);
                return this.buildAuthenticatorWithPassword();
            }
        }).orElseGet(() -> {  // no token available, also using password-based
            AuthenticationHandler.LOGGER.debug("Token not available, using password.");
            return this.buildAuthenticatorWithPassword();
        });
    }

    boolean storeToken(final ZonkyApiToken token) throws JAXBException {
        final String marshalled = ZonkyApiToken.marshal(token);
        if (this.data.setToken(new StringReader(marshalled))) {
            AuthenticationHandler.LOGGER.debug("Token stored.");
            return true;
        } else {
            AuthenticationHandler.LOGGER.debug("Failed storing token.");
            return false;
        }
    }

    /**
     * Decide whether or not to log out, based on user preferences.
     *
     * @param token The token to use for the next login, if enabled.
     * @return True if RoboZonky should log out, false otherwise.
     */
    private boolean isLogoutAllowed(final ZonkyApiToken token) {
        if (!this.tokenBased) { // not using token; always logout
            return true;
        }
        try { // store token so that, in case of token refresh or new token, we always have the latest data
            return !this.storeToken(token);
        } catch (final Exception ex) {
            AuthenticationHandler.LOGGER.info("Access token not written, will need to use password next time.", ex);
            this.data.deleteToken();
            return true;
        }
    }

    /**
     * Execute investment operation over authenticated API.
     *
     * @param provider API provider to be used for constructing the authenticated API.
     * @param operation Operation to execute over the API.
     * @return Investments newly made through the API.
     * @throws RuntimeException Some exception from RESTEasy when Zonky login fails.
     */
    public Collection<Investment> execute(final ApiProvider provider,
                                          final Function<ZonkyApi, Collection<Investment>> operation) {
        final Authentication currentAuthentication = this.buildAuthenticator().apply(provider);
        try (final AbstractApiProvider.ApiWrapper<ZonkyApi> apiWrapper = currentAuthentication.getZonkyApi()) {
            try {
                return apiWrapper.execute(operation);
            } finally { // attempt to log out no matter what happens
                final boolean logoutAllowed = this.isLogoutAllowed(currentAuthentication.getZonkyApiToken());
                if (logoutAllowed) {
                    AuthenticationHandler.LOGGER.info("Logging out.");
                    apiWrapper.execute(ZonkyApi::logout);
                } else { // if we're using the token, we should never log out
                    AuthenticationHandler.LOGGER.info("Refresh token needs to be reused, not logging out of Zonky.");
                }
            }
        }
    }

}
