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

package com.github.triceo.robozonky.app.authentication;

import java.io.Reader;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
import javax.xml.bind.JAXBException;

import com.github.triceo.robozonky.authentication.Authenticator;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationHandler.class);

    /**
     * Build authentication mechanism that will keep the session alive via the use of session token. The mechanism will
     * never log out, but the session may expire if not refresh regularly. This is potentially unsafe, as it will
     * eventually store a plain-text access token on the hard drive, for everyone to see.
     *
     * @param data Provider for the sensitive information, such as passwords and tokens.
     * @return The desired authentication method.
     */
    public static AuthenticationHandler tokenBased(final SensitiveInformationProvider data) {
        return new AuthenticationHandler(data, true);
    }

    /**
     * Build authentication mechanism that will log out at the end of RoboZonky's operations. This will ignore the
     * access tokens.
     *
     * @param data Provider for the sensitive information, such as passwords and tokens.
     * @return The desired authentication method.
     */
    public static AuthenticationHandler passwordBased(final SensitiveInformationProvider data) {
        return new AuthenticationHandler(data, false);
    }

    private final boolean tokenBased;
    private final SensitiveInformationProvider data;
    private long tokenRefreshBeforeExpirationInSeconds = 60;

    private AuthenticationHandler(final SensitiveInformationProvider data, final boolean tokenBased) {
        this.data = data;
        this.tokenBased = tokenBased;
    }

    /**
     * Optionally provide the earliest time before the token expiration at which the access token should be refreshed.
     * The token will only be refreshed if RoboZonky is launched some time between then and the token expiration.
     *
     * If this method is not called, the default value is {@link #tokenRefreshBeforeExpirationInSeconds} seconds.
     *
     * @param time Access token will be refreshed after expiration minus this.
     * @param unit Unit of time applied to the previous argument.
     * @return This.
     */
    public AuthenticationHandler withTokenRefreshingBeforeExpiration(final long time, final TemporalUnit unit) {
        this.tokenRefreshBeforeExpirationInSeconds = Duration.of(time, unit).getSeconds();
        return this;
    }

    private Authenticator buildWithPassword(final boolean isDryRun) {
        return Authenticator.withCredentials(this.data.getUsername(), this.data.getPassword(), isDryRun);
    }

    /**
     * Based on information received until this point, decide on the proper authentication method.
     *
     * @param isDryRun Whether or not we should authenticate to an API that is not allowed to invest.
     * @return Authentication method matching user preferences.
     */
    public Authenticator build(final boolean isDryRun) {
        final Optional<Reader> tokenStream = this.data.getToken();
        if (!this.tokenBased) {
            AuthenticationHandler.LOGGER.debug("Password-based authentication requested.");
            if (!this.data.setToken()) {
                AuthenticationHandler.LOGGER.info("Failed to delete stale access token.");
            }
            return this.buildWithPassword(isDryRun);
        } else if (!tokenStream.isPresent()) { // no token available, also using password-based
            AuthenticationHandler.LOGGER.debug("Token file not available, using password-based authentication.");
            return this.buildWithPassword(isDryRun);
        }
        boolean deleteToken = false;
        try {
            final ZonkyApiToken token = ZonkyApiToken.unmarshal(tokenStream.get());
            final LocalDateTime obtained = this.data.getTokenSetDate().get();
            final LocalDateTime expires = obtained.plus(token.getExpiresIn(), ChronoUnit.SECONDS);
            AuthenticationHandler.LOGGER.debug("Token obtained on {}, expires on {}.", obtained, expires);
            final LocalDateTime now = LocalDateTime.now();
            if (expires.isBefore(now)) {
                AuthenticationHandler.LOGGER.debug("Token {} expired, using password-based authentication.",
                        token.getAccessToken());
                deleteToken = true;
                return this.buildWithPassword(isDryRun);
            }
            if (expires.minus(this.tokenRefreshBeforeExpirationInSeconds, ChronoUnit.SECONDS).isBefore(now)) {
                AuthenticationHandler.LOGGER.debug("Access token expiring, will be refreshed.");
                deleteToken = true;
                return Authenticator.withAccessTokenAndRefresh(this.data.getUsername(), token, isDryRun);
            } else {
                AuthenticationHandler.LOGGER.debug("Reusing access token.");
                return Authenticator.withAccessToken(this.data.getUsername(), token, isDryRun);
            }
        } catch (final JAXBException ex) {
            AuthenticationHandler.LOGGER.warn("Failed parsing token, using password-based authentication.", ex);
            deleteToken = true;
            return this.buildWithPassword(isDryRun);
        } finally {
            if (deleteToken && !this.data.setToken()) {
                AuthenticationHandler.LOGGER.warn("Failed deleting token, authentication may stop working.");
            }
        }
    }

    /**
     * Decide whether or not to log out, based on user preferences.
     *
     * @param token Token in question.
     * @return True if RoboZonky should log out, false otherwise.
     */
    public boolean processToken(final ZonkyApiToken token) {
        if (!this.tokenBased) { // not using token; always logout
            return true;
        }
        final Optional<Reader> tokenStream = this.data.getToken();
        if (tokenStream.isPresent()) { // token already exists, do not logout
            return false;
        } else try { // try to store token
            final String marshalled = ZonkyApiToken.marshal(token);
            final boolean tokenStored = this.data.setToken(new StringReader(marshalled));
            if (tokenStored) {
                AuthenticationHandler.LOGGER.debug("Token stored successfully.");
                return false;
            } else {
                AuthenticationHandler.LOGGER.debug("Failed storing token.");
                return true;
            }
        } catch (final JAXBException ex) {
            AuthenticationHandler.LOGGER.info("Failed writing access token, will need to use password next time.", ex);
            return true;
        }
    }

}
