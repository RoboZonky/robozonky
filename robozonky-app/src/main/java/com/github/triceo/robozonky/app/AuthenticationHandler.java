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

package com.github.triceo.robozonky.app;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import javax.xml.bind.JAXBException;

import com.github.triceo.robozonky.authentication.Authenticator;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationHandler.class);
    private static final File TOKEN_FILE = new File("robozonky.token");

    /**
     * Build authentication mechanism that will keep the session alive via the use of session token. The mechanism will
     * never log out, but the session may expire if not refresh regularly. This is potentially unsafe, as it will
     * eventually store a plain-text access token on the hard drive, for everyone to see.
     *
     * @param username Username to log in.
     * @return The desired authentication method.
     */
    public static AuthenticationHandler tokenBased(final String username) {
        return new AuthenticationHandler(username, true);
    }

    /**
     * Build authentication mechanism that will log out at the end of RoboZonky's operations. This will ignore the
     * access tokens.
     *
     * @param username Username to log in.
     * @return The desired authentication method.
     */
    public static AuthenticationHandler passwordBased(final String username) {
        return new AuthenticationHandler(username, false);
    }

    private final boolean tokenBased;
    private final String username;
    private long tokenRefreshBeforeExpirationInSeconds = 60;
    private String password = null;

    private AuthenticationHandler(final String username, final boolean tokenBased) {
        this.tokenBased = tokenBased;
        this.username = username;
    }

    /**
     * Optionally provide a password for the authentication method.
     *
     * @param password If not provided, based on the selected authentication method, RoboZonky may or may not log in.
     * @return This.
     */
    public AuthenticationHandler withPassword(final String password) {
        this.password = password;
        return this;
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

    /**
     * Based on information received until this point, decide on the proper authentication method.
     *
     * @return Authentication method matching user preferences.
     */
    public Authenticator build() {
        if (!this.tokenBased) {
            AuthenticationHandler.LOGGER.debug("Password-based authentication requested.");
            return Authenticator.withCredentials(this.username, this.password);
        } else if (!AuthenticationHandler.TOKEN_FILE.canRead()) { // no token available, also using password-based
            AuthenticationHandler.LOGGER.debug("Token file not available, using password-based authentication.");
            return Authenticator.withCredentials(this.username, this.password);
        }
        boolean deleteToken = false;
        try {
            final ZonkyApiToken token = ZonkyApiToken.unmarshal(AuthenticationHandler.TOKEN_FILE);
            final LocalDateTime obtained =
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(AuthenticationHandler.TOKEN_FILE.lastModified()),
                    ZoneId.systemDefault());
            final LocalDateTime expires = obtained.plus(token.getExpiresIn(), ChronoUnit.SECONDS);
            AuthenticationHandler.LOGGER.debug("Token obtained on {}, expires on {}.", obtained, expires);
            final LocalDateTime now = LocalDateTime.now();
            if (expires.isBefore(now)) {
                AuthenticationHandler.LOGGER.debug("Token {} expired, using password-based authentication.",
                        token.getAccessToken());
                deleteToken = true;
                return Authenticator.withCredentials(this.username, this.password);
            }
            if (expires.minus(this.tokenRefreshBeforeExpirationInSeconds, ChronoUnit.SECONDS).isBefore(now)) {
                AuthenticationHandler.LOGGER.debug("Token {} expiring, will be refreshed.", token.getAccessToken());
                deleteToken = true;
                return Authenticator.withAccessTokenAndRefresh(this.username, token);
            } else {
                AuthenticationHandler.LOGGER.debug("Reusing access token {}.", token.getAccessToken());
                return Authenticator.withAccessToken(this.username, token);
            }
        } catch (final JAXBException ex) {
            AuthenticationHandler.LOGGER.warn("Failed parsing token file, using password-based authentication.", ex);
            deleteToken = true;
            return Authenticator.withCredentials(this.username, this.password);
        } finally {
            if (deleteToken && !AuthenticationHandler.TOKEN_FILE.delete()) {
                AuthenticationHandler.LOGGER.warn("Failed deleting token file, authentication may stop working.");
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
        } else if (AuthenticationHandler.TOKEN_FILE.canRead()) { // token already exists, do not logout
            return false;
        } else try { // try to store token
            AuthenticationHandler.TOKEN_FILE.delete(); // just to be sure
            ZonkyApiToken.marshal(token, AuthenticationHandler.TOKEN_FILE);
            return false;
        } catch (final JAXBException ex) {
            AuthenticationHandler.LOGGER.info("Failed writing access token, will need to use password next time.", ex);
            return true;
        }
    }

}
