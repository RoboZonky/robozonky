/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.cli;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.secrets.KeyStoreHandler;
import com.github.robozonky.internal.secrets.SecretProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "zonky-credentials", description = ZonkyPasswordFeature.DESCRIPTION)
public final class ZonkyPasswordFeature extends KeyStoreLeveragingFeature {

    private static final Logger LOGGER = LogManager.getLogger(ZonkyPasswordFeature.class);

    static final String DESCRIPTION = "Set credentials to access Zonky servers.";
    private final ApiProvider api;
    @CommandLine.Option(names = {"-u", "--username"},
            description = "Username to use to authenticate with Zonky servers.", required = true)
    private String username = null;
    @CommandLine.Option(names = {"-p", "--password"},
            description = "Authorization code obtained from Zonky.", required = true, interactive = true,
            arity = "0..1")
    private char[] password = null;

    public ZonkyPasswordFeature(final File keystore, final char[] keystoreSecret, final String username,
                                final char... password) {
        this(new ApiProvider(), keystore, keystoreSecret, username, password);
    }

    ZonkyPasswordFeature(final ApiProvider apiProvider, final File keystore, final char[] keystoreSecret,
                         final String username, final char... password) {
        super(keystore, keystoreSecret);
        this.api = apiProvider;
        this.username = username;
        this.password = password.clone();
    }

    ZonkyPasswordFeature() {
        // for Picocli
        this.api = new ApiProvider();
    }

    public static void attemptLoginAndStore(final KeyStoreHandler keyStoreHandler, final ApiProvider api,
                                            final String username, final char... password) {
        LOGGER.debug("Logging into Zonky.");
        final ZonkyApiToken token = api.oauth(oauth -> oauth.login(password));
        final SecretProvider secretProvider = SecretProvider.keyStoreBased(keyStoreHandler, username, password);
        secretProvider.setToken(token);
        LOGGER.debug("Token stored.");
    }

    @Override
    public String describe() {
        return DESCRIPTION;
    }

    @Override
    public void setup() throws SetupFailedException {
        try {
            super.setup();
            attemptLoginAndStore(getStorage(), api, username, password);
        } catch (final Exception ex) {
            throw new SetupFailedException(ex);
        }
    }

    @Override
    public void test() throws TestFailedException {
        try {
            super.test();
            final SecretProvider s = SecretProvider.keyStoreBased(this.getStorage());
            final ZonkyApiToken newToken = s.getToken()
                    .map(token -> api.oauth(oAuth -> oAuth.refresh(token)))
                    .orElseThrow(() -> new IllegalStateException("Zonky API token missing."));
            s.setToken(newToken);
            LOGGER.info("Access token for '{}' will expire on {}.", s.getUsername(), newToken.getExpiresOn());
        } catch (final Exception ex) {
            throw new TestFailedException(ex);
        }
    }
}
