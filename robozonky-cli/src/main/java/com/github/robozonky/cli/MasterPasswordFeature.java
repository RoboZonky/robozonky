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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames = "master-password", commandDescription = MasterPasswordFeature.DESCRIPTION)
public class MasterPasswordFeature extends KeyStoreLeveragingFeature {

    static final String DESCRIPTION = "Change master secret to the RoboZonky keystore.";
    @Parameter(order = 3, names = {"-n", "--new-secret"}, converter = PasswordConverter.class,
            description = "Username to use to authenticate with Zonky servers.", required = true)
    private char[] newSecret = null;

    public MasterPasswordFeature(final File keystore, final char[] keystoreSecret, final char... newSecret) {
        super(keystore, keystoreSecret);
        this.newSecret = newSecret.clone();
    }

    MasterPasswordFeature() {
        // for JCommander
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
