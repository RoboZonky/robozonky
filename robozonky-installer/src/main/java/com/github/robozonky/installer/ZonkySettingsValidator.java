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

package com.github.robozonky.installer;

import com.github.robozonky.cli.ZonkyCredentialsFeature;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.secrets.KeyStoreHandler;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class ZonkySettingsValidator extends AbstractValidator {

    private final Supplier<ApiProvider> apiSupplier;

    /**
     * Default constructor for the IZPack.
     */
    public ZonkySettingsValidator() {
        this(ApiProvider::new);
    }

    /**
     * This constructor exists for testing purposes.
     * @param apiSupplier Will provide the APIs for this class.
     */
    ZonkySettingsValidator(final Supplier<ApiProvider> apiSupplier) {
        this.apiSupplier = apiSupplier;
    }

    @Override
    public DataValidator.Status validateDataPossiblyThrowingException(final InstallData installData) throws IOException {
        final File f = File.createTempFile("robozonky", "keystore");
        logger.debug("Created temporary keystore file {}.", f.getAbsolutePath());
        Files.delete(f.toPath()); // or else the next step fails
        final String keystoreType = Variables.KEYSTORE_TYPE.getValue(installData);
        if (Objects.equals(keystoreType, "file")) { // use existing token
            final String keystorePath = Variables.KEYSTORE_PATH.getValue(installData);
            logger.info("Will use existing keystore {}.", keystorePath);
            final char[] password = Variables.KEYSTORE_PASSWORD.getValue(installData).toCharArray();
            try {
                Files.copy(Path.of(keystorePath), f.toPath());
                final KeyStoreHandler k = KeyStoreHandler.open(f, password);
                logger.debug("Keystore open, attempting token refresh.");
                ZonkyCredentialsFeature.refreshToken(k, apiSupplier.get());
                logger.debug("Over and out.");
                RoboZonkyInstallerListener.setKeystoreInformation(f, password);
                return DataValidator.Status.OK;
            } catch (final Exception t) {
                logger.warn("Failed obtaining Zonky API token.", t);
                return Status.WARNING;
            }
        } else {  // fresh login
            final String username = Variables.ZONKY_USERNAME.getValue(installData);
            logger.info("Will use new authorization code for '{}'.", username);
            final String password = Variables.ZONKY_PASSWORD.getValue(installData);
            try {
                final char[] p = UUID.randomUUID().toString().toCharArray();
                final KeyStoreHandler k = KeyStoreHandler.create(f, p);
                logger.debug("Keystore created, attempting login.");
                ZonkyCredentialsFeature.attemptLoginAndStore(k, apiSupplier.get(), username, password.toCharArray());
                logger.debug("Over and out.");
                RoboZonkyInstallerListener.setKeystoreInformation(f, p);
                return DataValidator.Status.OK;
            } catch (final Exception t) {
                logger.warn("Failed obtaining Zonky API token.", t);
                return DataValidator.Status.ERROR;
            }
        }
    }

    @Override
    public String getErrorMessageId() {
        return "Přihlašovací udaje nebylo možné ověřit, instalace nemůže pokračovat.";
    }

    @Override
    public String getWarningMessageId() {
        return "Přihlášení existujícím klíčem se nezdařilo. Ujistěte se, že heslo je správné a platnost klíče " +
                "nevypršela, nebo zvolte přihlášení novým autorizačním kódem.";
    }
}
