/*
 * Copyright 2018 The RoboZonky Project
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

import java.io.File;
import java.util.Optional;

import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import picocli.CommandLine;

@CommandLine.Command(name = "zonky-credentials", description = ZonkyPasswordFeature.DESCRIPTION)
public final class ZonkyPasswordFeature extends KeyStoreLeveragingFeature {

    static final String DESCRIPTION = "Set credentials to access Zonky servers.";
    private final ApiProvider api;
    @CommandLine.Option(names = {"-u", "--username"},
            description = "Username to use to authenticate with Zonky servers.", required = true)
    private String username = null;
    @CommandLine.Option(names = {"-p", "--password"},
            description = "Password to use to authenticate with Zonky servers.", required = true, interactive = true)
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

    public static void attemptLogin(final ApiProvider api, final String username,
                                    final char... password) throws TestFailedException {
        final Optional<Exception> thrown = api.oauth(oauth -> {
            try {
                api.run(Zonky::logout, () -> oauth.login(username, password));
                return Optional.empty();
            } catch (final Exception ex) {
                return Optional.of(ex);
            }
        });
        if (thrown.isPresent()) {
            throw new TestFailedException(thrown.get());
        }
    }

    @Override
    public String describe() {
        return DESCRIPTION;
    }

    @Override
    public void setup() throws SetupFailedException {
        super.setup();
        SecretProvider.keyStoreBased(this.getStorage(), username, password);
    }

    @Override
    public void test() throws TestFailedException {
        super.test();
        final SecretProvider s = SecretProvider.keyStoreBased(this.getStorage());
        attemptLogin(api, s.getUsername(), s.getPassword());
    }
}
