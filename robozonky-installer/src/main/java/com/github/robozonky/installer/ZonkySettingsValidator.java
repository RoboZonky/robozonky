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

import com.github.robozonky.cli.ZonkyPasswordFeature;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.secrets.KeyStoreHandler;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

import java.io.File;
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
    public DataValidator.Status validateDataPossiblyThrowingException(final InstallData installData) {
        final String username = Variables.ZONKY_USERNAME.getValue(installData);
        final String password = Variables.ZONKY_PASSWORD.getValue(installData);
        try {
            final File f = File.createTempFile("robozonky", "keystore");
            final char[] p = UUID.randomUUID().toString().toCharArray();
            f.delete(); // or else the next step fails
            final KeyStoreHandler k = KeyStoreHandler.create(f, p);
            ZonkyPasswordFeature.attemptLoginAndStore(k, apiSupplier.get(), username, password.toCharArray());
            RoboZonkyInstallerListener.setKeystoreInformation(f, p);
            return DataValidator.Status.OK;
        } catch (final Exception t) {
            logger.warn("Failed obtaining Zonky API token.", t);
            return DataValidator.Status.ERROR;
        }
    }

    @Override
    public String getErrorMessageId() {
        return "Přihlašovací udaje nebylo možné ověřit, instalace nemůže pokračovat.";
    }

}
