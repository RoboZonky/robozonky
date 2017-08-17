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

package com.github.triceo.robozonky.installer.panels;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.common.extensions.Checker;
import com.github.triceo.robozonky.common.extensions.ConfirmationProviderLoader;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

public class ZonkoidSettingsValidator extends AbstractValidator {

    private final Supplier<Optional<ConfirmationProvider>> zonkoidSupplier;

    public ZonkoidSettingsValidator() {
        this(() -> ConfirmationProviderLoader.load("zonkoid"));
    }

    /**
     * This constructor exists for testing purposes.
     * @param zonkoidSupplier Confirmation provider for this class.
     */
    ZonkoidSettingsValidator(final Supplier<Optional<ConfirmationProvider>> zonkoidSupplier) {
        this.zonkoidSupplier = zonkoidSupplier;
    }

    @Override
    public DataValidator.Status validateDataPossiblyThrowingException(final InstallData installData) {
        return zonkoidSupplier.get()
                .map(zonkoid -> {
                    final String username = Variables.ZONKY_USERNAME.getValue(installData);
                    final char[] token = Variables.ZONKOID_TOKEN.getValue(installData).toCharArray();
                    if (Checker.confirmations(zonkoid, username, token)) {
                        return DataValidator.Status.OK;
                    } else {
                        return DataValidator.Status.WARNING;
                    }
                }).orElse(DataValidator.Status.ERROR);
    }

    @Override
    public String getErrorMessageId() {
        return "Nepodařilo se odeslat notifikaci do mobilní aplikace. Kód nebylo možné ověřit.";
    }

    @Override
    public String getWarningMessageId() {
        return "Došlo k chybě při kontaktování serveru Zonkoid. " +
                "Budete-li pokračovat, mobilní notifikace nemusí fungovat správně.";
    }
}
