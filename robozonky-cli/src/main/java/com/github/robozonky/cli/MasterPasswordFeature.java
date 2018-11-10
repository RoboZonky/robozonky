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
import java.io.IOException;

import picocli.CommandLine;

@CommandLine.Command(name = "master-password", description = MasterPasswordFeature.DESCRIPTION)
public final class MasterPasswordFeature extends KeyStoreLeveragingFeature {

    static final String DESCRIPTION = "Change password of the master keystore.";
    @CommandLine.Option(names = {"-n", "--new-secret"},
            description = "Username to use to authenticate with Zonky servers.", required = true, interactive = true)
    private char[] newSecret = null;

    public MasterPasswordFeature(final File keystore, final char[] keystoreSecret, final char... newSecret) {
        super(keystore, keystoreSecret);
        this.newSecret = newSecret.clone();
    }

    MasterPasswordFeature() {
        // for Picocli
    }

    @Override
    public String describe() {
        return DESCRIPTION;
    }

    @Override
    public void setup() throws SetupFailedException {
        super.setup();
        try {
            this.getStorage().save(newSecret);
        } catch (final IOException ex) {
            throw new SetupFailedException(ex);
        }
    }

    @Override
    public void test() throws TestFailedException {
        super.test(newSecret);
    }
}
