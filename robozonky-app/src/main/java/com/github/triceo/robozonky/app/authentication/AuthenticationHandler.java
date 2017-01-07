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

import java.io.Reader;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import javax.ws.rs.BadRequestException;
import javax.xml.bind.JAXBException;

import com.github.triceo.robozonky.ApiProvider;
import com.github.triceo.robozonky.Authentication;
import com.github.triceo.robozonky.Authenticator;
import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
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

    private Authenticator buildWithPassword() {
        if (!this.data.deleteToken()) { // get rid of any stale token
            AuthenticationHandler.LOGGER.warn("Failed deleting token.");
        }
        return Authenticator.withCredentials(this.data.getUsername(), this.data.getPassword());
    }

    /**
     * Based on information received until this point, decide on the proper authentication method.
     *
     * @return Authentication method matching user preferences.
     */
    private Authenticator build() {
        if (!this.tokenBased) {
            AuthenticationHandler.LOGGER.debug("Password-based authentication requested.");
            return this.buildWithPassword();
        }
        return this.data.getToken().map(r -> {
            try {
                final ZonkyApiToken token = ZonkyApiToken.unmarshal(r);
                final OffsetDateTime obtained = this.data.getTokenSetDate()
                        .orElse(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
                final OffsetDateTime expires = obtained.plus(token.getExpiresIn(), ChronoUnit.SECONDS);
                AuthenticationHandler.LOGGER.debug("Token obtained on {}, expires on {}.", obtained, expires);
                final OffsetDateTime now = OffsetDateTime.now();
                if (expires.isBefore(now)) {
                    AuthenticationHandler.LOGGER.debug("Token expired, using password-based authentication.");
                    return this.buildWithPassword();
                } else if (expires.minus(this.tokenRefreshBeforeExpiration).isBefore(now)) {
                    AuthenticationHandler.LOGGER.debug("Access token expiring, will be refreshed.");
                    return Authenticator.withAccessTokenAndRefresh(this.data.getUsername(), token);
                } else {
                    AuthenticationHandler.LOGGER.debug("Reusing access token.");
                    return Authenticator.withAccessToken(this.data.getUsername(), token);
                }
            } catch (final Exception ex) {
                AuthenticationHandler.LOGGER.warn("Failed parsing token, using password-based authentication.", ex);
                return this.buildWithPassword();
            }
        }).orElseGet(() -> {  // no token available, also using password-based
            AuthenticationHandler.LOGGER.debug("Token not available, using password-based authentication.");
            return this.buildWithPassword();
        });
    }

    boolean storeToken(final ZonkyApiToken token) throws JAXBException {
        final String marshalled = ZonkyApiToken.marshal(token);
        final boolean tokenStored = this.data.setToken(new StringReader(marshalled));
        if (!tokenStored) {
            AuthenticationHandler.LOGGER.debug("Failed storing token.");
        }
        return tokenStored;
    }

    /**
     * Decide whether or not to log out, based on user preferences.
     *
     * @param token Token in question.
     * @return True if RoboZonky should log out, false otherwise.
     */
    private boolean isLogoutAllowed(final ZonkyApiToken token) {
        if (!this.tokenBased) { // not using token; always logout
            return true;
        }
        final Optional<Reader> tokenStream = this.data.getToken();
        if (tokenStream.isPresent()) { // token already exists, do not logout
            return false;
        } else try {
            return !this.storeToken(token);
        } catch (final Exception ex) {
            AuthenticationHandler.LOGGER.info("Failed writing access token, will need to use password next time.", ex);
            return true;
        }
    }

    public Optional<Collection<Investment>> execute(final ApiProvider provider,
                                                    final Function<ZonkyApi, Collection<Investment>> operation) {
        final Authentication login;
        try { // catch this exception here, so that anything coming from the invest() method can be thrown separately
            login = this.build().authenticate(provider);
        } catch (final BadRequestException ex) {
            AuthenticationHandler.LOGGER.error("Failed authenticating with Zonky.", ex);
            return Optional.empty();
        }
        final Collection<Investment> result = operation.apply(login.getZonkyApi());
        try { // log out and ignore any resulting error
            final boolean logoutAllowed = this.isLogoutAllowed(login.getZonkyApiToken());
            if (logoutAllowed) {
                login.getZonkyApi().logout();
            } else { // if we're using the token, we should never log out
                AuthenticationHandler.LOGGER.info("Refresh token needs to be reused, not logging out of Zonky.");
            }
        } catch (final RuntimeException ex) {
            AuthenticationHandler.LOGGER.warn("Failed logging out of Zonky.", ex);
        }
        return Optional.of(result);
    }

}
