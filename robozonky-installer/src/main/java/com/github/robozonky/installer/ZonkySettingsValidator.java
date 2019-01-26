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

import java.util.function.Supplier;
import javax.ws.rs.ServerErrorException;

import com.github.robozonky.cli.TestFailedException;
import com.github.robozonky.cli.ZonkyPasswordFeature;
import com.github.robozonky.common.remote.ApiProvider;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

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
            ZonkyPasswordFeature.attemptLogin(apiSupplier.get(), username, password.toCharArray());
            return DataValidator.Status.OK;
        } catch (final TestFailedException t) {
            if (t.getCause() instanceof ServerErrorException) {
                logger.error("Failed accessing Zonky.", t);
                return DataValidator.Status.ERROR;
            } else {
                logger.warn("Failed logging in.", t);
                return DataValidator.Status.WARNING;
            }
        }
    }

    @Override
    public String getErrorMessageId() {
        return "Došlo k chybě při komunikaci se Zonky. " +
                "Přihlašovací udaje nebylo možné ověřit.";
    }

    @Override
    public String getWarningMessageId() {
        return "Došlo k chybě při ověřování přihlašovacích udajů Zonky. " +
                "Budete-li pokračovat, RoboZonky nemusí fungovat správně.";
    }
}
