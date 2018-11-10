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

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.common.extensions.ConfirmationProviderLoader;
import com.github.robozonky.common.secrets.SecretProvider;
import picocli.CommandLine;

@CommandLine.Command(name = "zonkoid-credentials", description = ZonkoidPasswordFeature.DESCRIPTION)
public final class ZonkoidPasswordFeature extends KeyStoreLeveragingFeature {

    static final String DESCRIPTION = "Set credentials to access Zonkoid.";
    static final String ZONKOID_ID = "zonkoid";
    private final String id;
    @CommandLine.Option(names = {"-p", "--password"},
            description = "Code generated in the Zonkoid mobile application.", required = true, interactive = true)
    private char[] password = null;

    public ZonkoidPasswordFeature(final File keystore, final char[] keystoreSecret, final char... password) {
        this(ZONKOID_ID, keystore, keystoreSecret, password);
    }

    ZonkoidPasswordFeature(final String id, final File keystore, final char[] keystoreSecret, final char... password) {
        super(keystore, keystoreSecret);
        this.id = id;
        this.password = password.clone();
    }

    ZonkoidPasswordFeature() {
        // for Picocli
        this.id = ZONKOID_ID;
    }

    @Override
    public String describe() {
        return DESCRIPTION;
    }

    @Override
    public void setup() throws SetupFailedException {
        super.setup();
        final SecretProvider s = SecretProvider.keyStoreBased(this.getStorage());
        try {
            s.getUsername(); // ensure we have Zonky username prepared
            s.setSecret(id, password);
        } catch (final Exception ex) {
            throw new SetupFailedException(ex);
        }
    }

    @Override
    public void test() throws TestFailedException {
        super.test();
        final SecretProvider s = SecretProvider.keyStoreBased(this.getStorage());
        final Optional<ConfirmationProvider> zonkoid = ConfirmationProviderLoader.load(id);
        if (zonkoid.isPresent()) {
            if (!Checker.confirmations(zonkoid.get(), s.getUsername(), s.getSecret(id).get())) {
                throw new TestFailedException("Could not connect to Zonkoid, check log for details.");
            }
        } else {
            throw new TestFailedException("Could not find Zonkoid provider.");
        }
    }
}
