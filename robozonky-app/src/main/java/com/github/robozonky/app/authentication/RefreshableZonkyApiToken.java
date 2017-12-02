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

import java.io.Reader;
import java.io.StringReader;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.BadRequestException;
import javax.xml.bind.JAXBException;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.secrets.SecretProvider;

/**
 * Will keep permanent user authentication running in the background.
 */
class RefreshableZonkyApiToken extends Refreshable<ZonkyApiToken> {

    private final SecretProvider secrets;
    private final ApiProvider apis;

    public RefreshableZonkyApiToken(final ApiProvider apis, final SecretProvider secrets) {
        this.apis = apis;
        this.secrets = secrets;
    }

    private static Reader tokenToReader(final ZonkyApiToken token) throws JAXBException {
        return new StringReader(ZonkyApiToken.marshal(token));
    }

    private ZonkyApiToken withToken(final ZonkyApiToken token) {
        LOGGER.info("Authenticating as '{}', refreshing access token.", secrets.getUsername());
        try {
            return apis.oauth((oauth) -> oauth.refresh(token));
        } catch (final BadRequestException ex) { // possibly just an expired token, retry with password
            LOGGER.debug("Failed refreshing access token, using password.");
            return withPassword();
        }
    }

    private ZonkyApiToken withPassword() {
        return PasswordBasedAccess.trigger(apis, secrets.getUsername(), secrets.getPassword());
    }

    @Override
    protected Optional<String> getLatestSource() {
        return Optional.of(UUID.randomUUID().toString()); // refresh every time it is scheduled
    }

    @Override
    protected Optional<ZonkyApiToken> transform(final String source) {
        try {
            final ZonkyApiToken newToken = this.getLatest(Duration.ofNanos(1)) // don't wait if refreshable not run yet
                    .map(this::withToken) // subsequent runs = token refresh
                    .orElseGet(this::withPassword); // first run = password-based auth
            try { // store token so that it can be retrieved back in case of daemon restart
                secrets.setToken(RefreshableZonkyApiToken.tokenToReader(newToken));
                LOGGER.debug("New token stored, expires on {}.", newToken.getExpiresOn());
            } catch (final JAXBException ex) {
                LOGGER.debug("Failed storing token into secure storage, may need to use password next time.", ex);
            }
            return Optional.of(newToken);
        } catch (final Exception ex) {
            LOGGER.warn("Authentication failed.", ex);
            return Optional.empty();
        }
    }
}
