/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.configuration;

import java.io.File;
import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SecretProvider;

class AuthenticationCommandLineFragment implements CommandLineFragment {

    @Parameter(names = {"-u", "--username"},
            description = "Used to connect to the Zonky server.")
    private String username = null;

    @Parameter(names = {"-p", "--password"}, required = true,
            description = "Used to connect to the Zonky server, or to read the secure storage.",
            converter = PasswordConverter.class)
    private char[] password = null;

    @Parameter(names = {"-g", "--guarded"},
            description = "Path to secure file that contains username, password etc.")
    private File keystore = null;

    @Parameter(names = {"-r", "--refresh"},
            description = "Once logged in, RoboZonky will never log out unless login expires. Use with caution.")
    private boolean refreshTokenEnabled = false;

    public AuthenticationCommandLineFragment() {
        // do nothing
    }

    AuthenticationCommandLineFragment(final String username, final File keystore) {
        this.username = username;
        this.keystore = keystore;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    public char[] getPassword() {
        return password;
    }

    public Optional<File> getKeystore() {
        return Optional.ofNullable(keystore);
    }

    public AuthenticationHandler createAuthenticationHandler(final SecretProvider secrets) {
        if (refreshTokenEnabled) { // FIXME figure out refresh interval
            final TemporalAmount duration = Duration.ofSeconds(60);
            return AuthenticationHandler.tokenBased(secrets, duration);
        } else {
            return AuthenticationHandler.passwordBased(secrets);
        }
    }


    @Override
    public void validate() throws ParameterException {
        if (!this.getKeystore().isPresent() && !this.getUsername().isPresent()) {
            throw new ParameterException("Either --username or --guarded parameter must be specified.");
        } else if (this.getKeystore().isPresent() && this.getUsername().isPresent()) {
            throw new ParameterException("Only one of --username or --guarded parameters must be specified.");
        }
    }
}
