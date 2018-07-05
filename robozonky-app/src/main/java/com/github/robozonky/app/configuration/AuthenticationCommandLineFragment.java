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

package com.github.robozonky.app.configuration;

import java.io.File;
import java.util.Optional;

import com.beust.jcommander.Parameter;

class AuthenticationCommandLineFragment extends AbstractCommandLineFragment {

    @Parameter(names = {"-p", "--password"}, password = true, required = true,
            description = "Enter Zonky account password or secure storage password.",
            converter = PasswordConverter.class)
    private char[] password = null;

    @Parameter(names = {"-g", "--guarded"},
            description = "Path to secure file that contains username, password etc.", required = true)
    private File keystore = null;

    public AuthenticationCommandLineFragment() {
        // do nothing
    }

    AuthenticationCommandLineFragment(final File keystore) {
        this.keystore = keystore;
    }

    public char[] getPassword() {
        return password.clone();
    }

    public Optional<File> getKeystore() {
        return Optional.ofNullable(keystore);
    }
}
