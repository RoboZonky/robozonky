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
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.secrets.KeyStoreHandler;
import com.github.robozonky.internal.secrets.SecretProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(name = "zonky-credentials", description = ZonkyCredentialsFeature.DESCRIPTION)
public final class ZonkyCredentialsFeature extends KeyStoreLeveragingFeature {

    private static final Logger LOGGER = LogManager.getLogger(ZonkyCredentialsFeature.class);

    static final String DESCRIPTION = "Set credentials to access Zonky servers.";
    private final ApiProvider api;
    @CommandLine.Option(names = {"-u", "--username"},
            description = "Username to use to authenticate with Zonky servers.", required = true)
    private String username = null;
    @CommandLine.Option(names = {"-p", "--password"},
            description = "Authorization code obtained from Zonky. If not provided, will check for existing token",
            interactive = true, arity = "0..1")
    private char[] password = null;
    @CommandLine.Option(names = {"-t", "--token"},
            description = "Raw XML of the Zonky API token will be stored in this file. Keep it secret, keep it safe.")
    private Path tokenTargetPath = null;

    public ZonkyCredentialsFeature(final File keystore, final char[] keystoreSecret, final String username,
                                   final char... password) {
        this(new ApiProvider(), keystore, keystoreSecret, username, password);
    }

    ZonkyCredentialsFeature(final ApiProvider apiProvider, final File keystore, final char[] keystoreSecret,
                            final String username, final char... password) {
        super(keystore, keystoreSecret);
        this.api = apiProvider;
        this.username = username;
        this.password = password.clone();
    }

    ZonkyCredentialsFeature() {
        // for Picocli
        this.api = new ApiProvider();
    }

    public static void attemptLoginAndStore(final KeyStoreHandler keyStoreHandler, final ApiProvider api,
                                            final String username, final char... authorizationCode) {
        final SecretProvider secretProvider = SecretProvider.keyStoreBased(keyStoreHandler, username, authorizationCode);
        if (authorizationCode == null) {
            if (secretProvider.getToken().isEmpty()) {
                throw new IllegalStateException("No authorization code provided, yet no token available.");
            }
        } else {
            LOGGER.debug("Logging into Zonky.");
            final ZonkyApiToken token = api.oauth(oauth -> oauth.login(authorizationCode));
            secretProvider.setToken(token);
            LOGGER.debug("Token stored.");
        }
    }

    public static void refreshToken(final KeyStoreHandler keyStoreHandler, final ApiProvider api) {
        final SecretProvider s = SecretProvider.keyStoreBased(keyStoreHandler);
        final ZonkyApiToken newToken = s.getToken()
                .map(token -> api.oauth(oAuth -> oAuth.refresh(token)))
                .orElseThrow(() -> new IllegalStateException("Zonky API token missing."));
        s.setToken(newToken);
        LOGGER.info("Access token for '{}' will expire on {}.", s.getUsername(), newToken.getExpiresOn());
    }

    public static void outputToken(final KeyStoreHandler keyStoreHandler, final Path target) throws IOException {
        final SecretProvider s = SecretProvider.keyStoreBased(keyStoreHandler);
        final ZonkyApiToken token = s.getToken()
                .orElseThrow(() -> new IllegalStateException("Zonky API token missing."));
        Files.write(target, ZonkyApiToken.marshal(token).getBytes(Defaults.CHARSET));
        LOGGER.info("Raw token XML written to {}.", target);
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
            refreshToken(this.getStorage(), api);
            if (tokenTargetPath != null) {
                outputToken(this.getStorage(), tokenTargetPath);
            }
        } catch (final Exception ex) {
            throw new TestFailedException(ex);
        }
    }
}
