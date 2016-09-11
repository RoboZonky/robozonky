/*
 *
 *  Copyright 2016 Lukáš Petrovický
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.triceo.robozonky.app;

import java.net.SocketException;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.version.VersionCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static final ExecutorService HTTP_EXECUTOR = Executors.newWorkStealingPool();

    private static void exit(final ReturnCode returnCode, final Future<String> versionFuture) {
        App.HTTP_EXECUTOR.shutdown();
        App.newerRoboZonkyVersionExists(versionFuture);
        App.LOGGER.debug("RoboZonky terminating with '{}' return code.", returnCode);
        App.LOGGER.info("===== RoboZonky out. =====");
        System.exit(returnCode.getCode());
    }

    public static void main(final String... args) {
        App.LOGGER.info("RoboZonky v{} loading.", VersionCheck.retrieveCurrentVersion());
        App.LOGGER.debug("Running {} {} v{} on {} v{} ({}, {}).", System.getProperty("java.vendor"),
                System.getProperty("java.runtime.name"), System.getProperty("java.runtime.version"),
                System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"),
                Locale.getDefault());
        final Future<String> latestVersion = VersionCheck.retrieveLatestVersion(App.HTTP_EXECUTOR);
        boolean faultTolerant = false;
        try {
            final Optional<CommandLineInterface> optionalCli = CommandLineInterface.parse(args);
            if (!optionalCli.isPresent()) {
                App.exit(ReturnCode.ERROR_WRONG_PARAMETERS, latestVersion);
            }
            final CommandLineInterface cli = optionalCli.get();
            faultTolerant = cli.isFaultTolerant();
            if (faultTolerant) {
                App.LOGGER.info("RoboZonky is in fault-tolerant mode. Certain errors may not be reported as such.");
            }
            final Optional<AppContext> optionalCtx = cli.getCliOperatingMode().setup(cli);
            if (!optionalCtx.isPresent()) {
                App.exit(ReturnCode.ERROR_WRONG_PARAMETERS, latestVersion);
            }
            final AppContext ctx = optionalCtx.get();
            final Optional<AuthenticationHandler> optionalAuth = new AuthenticationHandlerProvider().apply(cli);
            if (!optionalAuth.isPresent()) {
                App.exit(ReturnCode.ERROR_SETUP, latestVersion);
            }
            App.LOGGER.info("===== RoboZonky at your service! =====");
            final boolean loginSucceeded = new Remote(ctx, optionalAuth.get()).call().isPresent();
            if (!loginSucceeded) {
                App.exit(ReturnCode.ERROR_SETUP, latestVersion);
            } else {
                App.exit(ReturnCode.OK, latestVersion);
            }
        } catch (final ProcessingException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof SocketException) {
                App.handleZonkyMaintenanceError(ex, latestVersion, faultTolerant);
            } else {
                App.handleUnexpectedError(ex, latestVersion);
            }
        } catch (final NotAllowedException ex) {
            App.handleZonkyMaintenanceError(ex, latestVersion, faultTolerant);
        } catch (final WebApplicationException ex) {
            App.LOGGER.error("Application encountered remote API error.", ex);
            App.exit(ReturnCode.ERROR_REMOTE, latestVersion);
        } catch (final RuntimeException ex) {
            App.handleUnexpectedError(ex, latestVersion);
        }
    }

    private static void handleUnexpectedError(final RuntimeException ex, final Future<String> latestVersion) {
        App.LOGGER.error("Unexpected error, likely RoboZonky bug." , ex);
        App.exit(ReturnCode.ERROR_UNEXPECTED, latestVersion);
    }

    private static void handleZonkyMaintenanceError(final RuntimeException ex, final Future<String> latestVersion,
                                                    final boolean faultTolerant) {
        App.LOGGER.warn("Application not allowed to access remote API, Zonky likely down for maintenance.", ex);
        App.exit(faultTolerant ? ReturnCode.OK : ReturnCode.ERROR_DOWN, latestVersion);
    }

    /**
     * Check the current version against a different version. Print log message with results.
     * @param futureVersion Version to check against.
     * @return True when a more recent version was found.
     */
    static boolean newerRoboZonkyVersionExists(final Future<String> futureVersion) {
        try {
            final String version = futureVersion.get();
            final boolean hasNewer = VersionCheck.isCurrentVersionOlderThan(version);
            if (hasNewer) {
                App.LOGGER.info("You are using an obsolete version of RoboZonky. Please upgrade to version {}.", version);
                return true;
            } else {
                App.LOGGER.info("Your version of RoboZonky seems up to date.");
                return false;
            }
        } catch (final InterruptedException | ExecutionException ex) {
            App.LOGGER.trace("Version check failed.", ex);
            return false;
        }
    }

}
