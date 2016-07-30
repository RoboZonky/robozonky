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

import com.github.triceo.robozonky.app.util.UnrecoverableRoboZonkyException;
import com.github.triceo.robozonky.authentication.Authentication;
import com.github.triceo.robozonky.authentication.Authenticator;
import com.github.triceo.robozonky.operations.LoginOperation;
import com.github.triceo.robozonky.operations.LogoutOperation;
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
     * The token will only be refreshed if RoboZonky is launched some time between the token expiration and 60 seconds
     * before then.
     *
     * @param data Provider for the sensitive information, such as passwords and tokens.
     * @param isDryRun Whether or not the API should be allowed to invest actual money.
     * @return The desired authentication method.
     */
    public static AuthenticationHandler tokenBased(final SensitiveInformationProvider data, final boolean isDryRun) {
        return new AuthenticationHandler(data, isDryRun, true);
    }

    /**
     * Build authentication mechanism that will keep the session alive via the use of session token. The mechanism will
     * never log out, but the session may expire if not refresh regularly. This is potentially unsafe, as it will
     * eventually store a plain-text access token on the hard drive, for everyone to see.
     *
     * The token will only be refreshed if RoboZonky is launched between token expiration and X second before token
     * expiration, where X comes from the arguments of this method.
     *
     * @param data Provider for the sensitive information, such as passwords and tokens.
     * @param isDryRun Whether or not the API should be allowed to invest actual money.
     * @param time Access token will be refreshed after expiration minus this.
     * @param unit Unit of time applied to the previous argument.
     * @return This.
     */
    public static AuthenticationHandler tokenBased(final SensitiveInformationProvider data, final boolean isDryRun,
                                                   final long time, final TemporalUnit unit) {
        return new AuthenticationHandler(data, isDryRun, Duration.of(time, unit).getSeconds(), true);
    }

    /**
     * Build authentication mechanism that will log out at the end of RoboZonky's operations. This will ignore the
     * access tokens.
     *
     * @param data Provider for the sensitive information, such as passwords and tokens.
     * @param isDryRun Whether or not the API should be allowed to invest actual money.
     * @return The desired authentication method.
     */
    public static AuthenticationHandler passwordBased(final SensitiveInformationProvider data, final boolean isDryRun) {
        return new AuthenticationHandler(data, isDryRun, false);
    }

    private final boolean tokenBased, dryRun;
    private final SensitiveInformationProvider data;
    private final long tokenRefreshBeforeExpirationInSeconds;

    private AuthenticationHandler(final SensitiveInformationProvider data, final boolean isDryRun,
                                  final boolean tokenBased) {
        this(data, isDryRun, 60, tokenBased);
    }

    private AuthenticationHandler(final SensitiveInformationProvider data, final boolean isDryRun,
                                  final long tokenRefreshBeforeExpirationInSeconds, final boolean tokenBased) {
        this.data = data;
        this.dryRun = isDryRun;
        this.tokenRefreshBeforeExpirationInSeconds = tokenRefreshBeforeExpirationInSeconds;
        this.tokenBased = tokenBased;
    }

    public boolean isTokenBased() {
        return tokenBased;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public long getTokenRefreshBeforeExpirationInSeconds() {
        return tokenRefreshBeforeExpirationInSeconds;
    }

    private Authenticator buildWithPassword() {
        return Authenticator.withCredentials(this.data.getUsername(), this.data.getPassword(), this.dryRun);
    }

    /**
     * Based on information received until this point, decide on the proper authentication method.
     *
     * @return Authentication method matching user preferences.
     */
    Authenticator build() {
        if (!this.tokenBased) {
            AuthenticationHandler.LOGGER.debug("Password-based authentication requested.");
            if (!this.data.deleteToken()) {
                AuthenticationHandler.LOGGER.info("Failed to delete stale access token.");
            }
            return this.buildWithPassword();
        }
        final Optional<Reader> tokenStream = this.data.getToken();
        if (!tokenStream.isPresent()) { // no token available, also using password-based
            AuthenticationHandler.LOGGER.debug("Token file not available, using password-based authentication.");
            return this.buildWithPassword();
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
                return this.buildWithPassword();
            }
            if (expires.minus(this.tokenRefreshBeforeExpirationInSeconds, ChronoUnit.SECONDS).isBefore(now)) {
                AuthenticationHandler.LOGGER.debug("Access token expiring, will be refreshed.");
                deleteToken = true;
                return Authenticator.withAccessTokenAndRefresh(this.data.getUsername(), token, this.dryRun);
            } else {
                AuthenticationHandler.LOGGER.debug("Reusing access token.");
                return Authenticator.withAccessToken(this.data.getUsername(), token, this.dryRun);
            }
        } catch (final JAXBException ex) {
            AuthenticationHandler.LOGGER.warn("Failed parsing token, using password-based authentication.", ex);
            deleteToken = true;
            return this.buildWithPassword();
        } finally {
            if (deleteToken && !this.data.deleteToken()) {
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
    boolean isLogoutAllowed(final ZonkyApiToken token) {
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

    /**
     * Log into Zonky API.
     *
     * @return Authenticated APIs.
     * @throws UnrecoverableRoboZonkyException When login fails.
     */
    public Authentication login() throws UnrecoverableRoboZonkyException {
        final Authenticator auth = this.build();
        final Optional<Authentication> possibleLogin = new LoginOperation().apply(auth);
        if (!possibleLogin.isPresent()) {
            throw new UnrecoverableRoboZonkyException("Login failed.");
        }
        return possibleLogin.get();
    }

    /**
     * Log out of the Zonky API, if necessary. Will not log out if there is an active token.
     *
     * @param login Previously returned by {@link #login()}.
     * @return True if it was decided to log out.
     */
    public boolean logout(final Authentication login) {
        final boolean logoutAllowed = this.isLogoutAllowed(login.getZonkyApiToken());
        if (logoutAllowed) {
            new LogoutOperation().apply(login.getZonkyApi());
            return true;
        } else { // if we're using the token, we should never log out
            AuthenticationHandler.LOGGER.info("Refresh token needs to be reused, not logging out of Zonky.");
            return false;
        }
    }

}
