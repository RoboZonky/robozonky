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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

abstract class AbstractValidator implements DataValidator {

    protected final Logger LOGGER = Logger.getLogger(this.getClass().getCanonicalName());

    protected abstract DataValidator.Status validateDataPossiblyThrowingException(InstallData installData);

    @Override
    public DataValidator.Status validateData(final InstallData installData) {
        try {
            return this.validateDataPossiblyThrowingException(installData);
        } catch (final Exception ex) { // the installer will never ever throw an exception (= neverending spinner)
            LOGGER.log(Level.SEVERE, "Uncaught exception.", ex);
            return DataValidator.Status.ERROR;
        }
    }

        @Override
    public boolean getDefaultAnswer() {
        return false;
    }
}
