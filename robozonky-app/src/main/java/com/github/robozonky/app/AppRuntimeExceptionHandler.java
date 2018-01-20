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

package com.github.robozonky.app;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.function.Consumer;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ServerErrorException;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.app.util.RuntimeExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AppRuntimeExceptionHandler extends RuntimeExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppRuntimeExceptionHandler.class);

    private static void handleException(final Throwable ex) {
        final Throwable cause = ex.getCause();
        if (ex instanceof NotAllowedException || ex instanceof ServerErrorException ||
                cause instanceof SocketException || cause instanceof UnknownHostException) {
            AppRuntimeExceptionHandler.handleZonkyMaintenanceError(ex);
        } else {
            AppRuntimeExceptionHandler.handleUnexpectedException(ex);
        }
    }

    private static void handleStandardException(final Throwable ex) {
        AppRuntimeExceptionHandler.LOGGER.error("Application encountered remote API error.", ex);
        App.exit(new ShutdownHook.Result(ReturnCode.ERROR_REMOTE, ex));
    }

    private static void handleUnexpectedException(final Throwable ex) {
        AppRuntimeExceptionHandler.LOGGER.error("Unexpected error, likely RoboZonky bug.", ex);
        App.exit(new ShutdownHook.Result(ReturnCode.ERROR_UNEXPECTED, ex));
    }

    private static void handleZonkyMaintenanceError(final Throwable ex) {
        AppRuntimeExceptionHandler.LOGGER
                .warn("Application not allowed to access remote API, Zonky likely down for maintenance.", ex);
        App.exit(new ShutdownHook.Result(ReturnCode.ERROR_DOWN, ex));
    }

    @Override
    protected Consumer<Throwable> getCommunicationFailureHandler() {
        return AppRuntimeExceptionHandler::handleException;
    }

    @Override
    protected Consumer<Throwable> getRemoteFailureHandler() {
        return AppRuntimeExceptionHandler::handleStandardException;
    }

    @Override
    protected Consumer<Throwable> getOtherFailureHandler() {
        return AppRuntimeExceptionHandler::handleUnexpectedException;
    }
}
