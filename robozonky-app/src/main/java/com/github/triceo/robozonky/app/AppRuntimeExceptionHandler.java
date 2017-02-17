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

package com.github.triceo.robozonky.app;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.function.Consumer;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ServerErrorException;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.app.util.RuntimeExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AppRuntimeExceptionHandler extends RuntimeExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppRuntimeExceptionHandler.class);

    private static void handleException(final Throwable ex, final boolean faultTolerant) {
        final Throwable cause = ex.getCause();
        if (ex instanceof NotAllowedException || ex instanceof ServerErrorException ||
                cause instanceof SocketException || cause instanceof UnknownHostException) {
            AppRuntimeExceptionHandler.handleZonkyMaintenanceError(ex, faultTolerant);
        } else {
            AppRuntimeExceptionHandler.handleUnexpectedException(ex);
        }
    }

    private static void handleException(final Throwable ex) {
        AppRuntimeExceptionHandler.LOGGER.error("Application encountered remote API error.", ex);
        App.exit(ReturnCode.ERROR_REMOTE, ex);
    }

    private static void handleUnexpectedException(final Throwable ex) {
        AppRuntimeExceptionHandler.LOGGER.error("Unexpected error, likely RoboZonky bug.", ex);
        App.exit(ReturnCode.ERROR_UNEXPECTED, ex);
    }

    private static void handleZonkyMaintenanceError(final Throwable ex, final boolean faultTolerant) {
        AppRuntimeExceptionHandler.LOGGER.warn("Application not allowed to access remote API, Zonky likely down for maintenance.", ex);
        if (faultTolerant) {
            AppRuntimeExceptionHandler.LOGGER.info("RoboZonky is in fault-tolerant mode. The above will not be reported as error.");
            App.exit(ReturnCode.OK, ex);
        } else {
            App.exit(ReturnCode.ERROR_DOWN, ex);
        }
    }

    private final boolean faultTolerant;

    public AppRuntimeExceptionHandler(final boolean faultTolerant) {
        this.faultTolerant = faultTolerant;
    }

    @Override
    protected Consumer<Throwable> getCommunicationFailureHandler() {
        return (in) -> AppRuntimeExceptionHandler.handleException(in, faultTolerant);
    }

    @Override
    protected Consumer<Throwable> getRemoteFailureHandler() {
        return AppRuntimeExceptionHandler::handleException;
    }

    @Override
    protected Consumer<Throwable> getOtherFailureHandler() {
        return AppRuntimeExceptionHandler::handleUnexpectedException;
    }

}
