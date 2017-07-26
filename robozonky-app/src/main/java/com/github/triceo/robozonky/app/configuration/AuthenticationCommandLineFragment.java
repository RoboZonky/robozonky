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
import java.time.temporal.TemporalAmount;
import java.util.Optional;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import com.github.triceo.robozonky.internal.api.Settings;

class AuthenticationCommandLineFragment extends AbstractCommandLineFragment {

    @Parameter(names = {"-u", "--username"},
            description = "Used to connect to the Zonky server.")
    private String username = null;

    @Parameter(names = {"-p", "--password"}, password = true, required = true,
            description = "Enter Zonky account password or secure storage password.",
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
        this(username, keystore, false);
    }

    AuthenticationCommandLineFragment(final String username, final File keystore, final boolean refreshTokenEnabled) {
        this.username = username;
        this.keystore = keystore;
        this.refreshTokenEnabled = refreshTokenEnabled;
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

    public Authenticated createAuthenticated(final SecretProvider secrets) {
        if (refreshTokenEnabled) {
            final TemporalAmount duration = Settings.INSTANCE.getTokenRefreshBeforeExpiration();
            return Authenticated.tokenBased(secrets, duration);
        } else {
            return Authenticated.passwordBased(secrets);
        }
    }

    private Optional<ParameterException> actuallyValidate() {
        if (!this.getKeystore().isPresent() && !this.getUsername().isPresent()) {
            return Optional.of(new ParameterException("Either --username or --guarded parameter must be used."));
        } else if (this.getKeystore().isPresent() && this.getUsername().isPresent()) {
            return Optional.of(new ParameterException("Only one of --username or --guarded parameters may be used."));
        }
        return Optional.empty();
    }

    @Override
    public void validate(final JCommander args) throws ParameterException {
        actuallyValidate().ifPresent(ex -> {
            ex.setJCommander(args);
            throw ex;
        });
    }
}
