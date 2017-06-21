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

package com.github.triceo.robozonky.installer.panels;

import java.net.SocketException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.ServerErrorException;

import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.common.remote.OAuth;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

public class ZonkySettingsValidator implements DataValidator {

    private static final Logger LOGGER = Logger.getLogger(ZonkySettingsValidator.class.getCanonicalName());

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
    public DataValidator.Status validateData(final InstallData installData) {
        final String username = Variables.ZONKY_USERNAME.getValue(installData);
        final String password = Variables.ZONKY_PASSWORD.getValue(installData);
        final ApiProvider p = apiSupplier.get();
        try (final OAuth oauth = p.oauth()) {
            ZonkySettingsValidator.LOGGER.info("Logging in.");
            final ZonkyApiToken token = oauth.login(username, password.toCharArray());
            ZonkySettingsValidator.LOGGER.info("Logging out.");
            try (final Zonky z = p.authenticated(token)) {
                z.logout();
            }
            return DataValidator.Status.OK;
        } catch (final Exception t) {
            if (t instanceof SocketException | t instanceof ServerErrorException) {
                ZonkySettingsValidator.LOGGER.log(Level.SEVERE, "Failed accessing Zonky.", t);
                return DataValidator.Status.ERROR;
            } else {
                ZonkySettingsValidator.LOGGER.log(Level.WARNING, "Failed logging in.", t);
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

    @Override
    public boolean getDefaultAnswer() {
        return false;
    }
}
