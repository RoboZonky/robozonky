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

import com.github.robozonky.internal.Settings;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

abstract class AbstractValidator implements DataValidator {

    protected final Logger logger = LogManager.getLogger(this.getClass());

    protected abstract DataValidator.Status validateDataPossiblyThrowingException(InstallData installData)
            throws Exception;

    private static Duration getTimeout() {
        Duration connection = Settings.INSTANCE.getConnectionTimeout();
        Duration socket = Settings.INSTANCE.getSocketTimeout();
        return connection.plus(socket).plusSeconds(10);
    }

    @Override
    public DataValidator.Status validateData(final InstallData installData) {
        try {
            final long timeoutInSeconds = getTimeout().toSeconds();
            logger.info("Starting background validation, will wait for up to {} seconds.", timeoutInSeconds);
            final Supplier<Status> c = () -> {
                try {
                    return this.validateDataPossiblyThrowingException(installData);
                } catch (final Exception ex) {
                    throw new IllegalStateException(ex);
                }
            };
            return CompletableFuture.supplyAsync(c)
                    .get(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (final Exception ex) { // the installer must never ever throw an exception (= neverending spinner)
            logger.error("Uncaught exception.", ex);
            return DataValidator.Status.ERROR;
        } finally {
            logger.info("Finished validation.");
        }
    }

    @Override
    public String getWarningMessageId() {
        return "";
    }

    @Override
    public boolean getDefaultAnswer() {
        return false;
    }
}
