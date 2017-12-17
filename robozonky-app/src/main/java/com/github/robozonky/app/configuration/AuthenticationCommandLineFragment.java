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
import java.time.Duration;
import java.util.Optional;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.internal.api.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AuthenticationCommandLineFragment extends AbstractCommandLineFragment {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationCommandLineFragment.class);

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

    @Deprecated
    @Parameter(names = {"-r", "--refresh"}, hidden = true)
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
            LOGGER.info("Persistent authentication is now enabled by default and can not be disabled."
                                + " '-r' command-line option will be removed in the next RoboZonky major version. " +
                                "Kindly stop using it.");
        }
        final Duration duration = Settings.INSTANCE.getTokenRefreshPeriod();
        return Authenticated.tokenBased(secrets, duration);
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
