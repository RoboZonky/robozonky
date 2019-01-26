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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class AbstractValidator implements DataValidator {

    protected final Logger logger = LogManager.getLogger(this.getClass());

    protected abstract DataValidator.Status validateDataPossiblyThrowingException(InstallData installData);

    @Override
    public DataValidator.Status validateData(final InstallData installData) {
        final ExecutorService e = Executors.newCachedThreadPool();
        try {
            logger.info("Starting validation.");
            final Callable<DataValidator.Status> c = () -> this.validateDataPossiblyThrowingException(installData);
            final Future<DataValidator.Status> f = e.submit(c);
            return f.get(15, TimeUnit.SECONDS); // don't wait for result indefinitely
        } catch (final Exception ex) { // the installer must never ever throw an exception (= neverending spinner)
            logger.error("Uncaught exception.", ex);
            return DataValidator.Status.ERROR;
        } finally {
            e.shutdownNow();
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
