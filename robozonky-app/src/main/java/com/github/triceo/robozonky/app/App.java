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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyStartingEvent;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.configuration.CommandLineInterface;
import com.github.triceo.robozonky.app.configuration.Configuration;
import com.github.triceo.robozonky.notifications.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * You are required to exit this app by calling {@link #exit(ReturnCode)}.
 */
public class App {

    static {
        // add process identification to log files
        MDC.put("process_id", ManagementFactory.getRuntimeMXBean().getName());
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static final File ROBOZONKY_LOCK = new File(System.getProperty("java.io.tmpdir"), "robozonky.lock");
    private static final ShutdownHook SHUTDOWN_HOOKS = new ShutdownHook();

    private static void exit(final ReturnCode returnCode) {
        App.exit(returnCode, null);
    }

    /**
     * Will terminate the application. Call this on every exit of the app to ensure proper termination. Failure to do
     * so may result in unpredictable behavior of this instance of RoboZonky or future ones.
     * @param returnCode Will be passed to {@link System#exit(int)}.
     * @param cause Exception that caused the application to exit, if any.
     */
    private static void exit(final ReturnCode returnCode, final Exception cause) {
        if (returnCode != ReturnCode.OK) {
            Events.fire(new RoboZonkyCrashedEvent(returnCode, cause));
        }
        App.SHUTDOWN_HOOKS.execute(returnCode);
        System.exit(returnCode.getCode());
    }

    static ReturnCode execute(final Configuration configuration, final AuthenticationHandler auth,
                              final Scheduler scheduler) {
        App.SHUTDOWN_HOOKS.register(new RoboZonkyStartupNotifier());
        configuration.getInvestmentStrategy()
                .ifPresent(refresher -> scheduler.submit(refresher, Duration.ofSeconds(60)));
        final boolean loginSucceeded = new Remote(configuration, auth).call().isPresent();
        return loginSucceeded ? ReturnCode.OK : ReturnCode.ERROR_SETUP;
    }

    public static void main(final String... args) {
        // make sure other RoboZonky processes are excluded
        if (!App.SHUTDOWN_HOOKS.register(new Exclusivity(App.ROBOZONKY_LOCK))) {
            App.exit(ReturnCode.ERROR_LOCK);
        }
        // and actually start running
        Events.fire(new RoboZonkyStartingEvent());
        App.LOGGER.info("RoboZonky v{} loading.", Defaults.ROBOZONKY_VERSION);
        App.LOGGER.debug("Running {} Java v{} on {} v{} ({}, {} CPUs, {}).", System.getProperty("java.vendor"),
                System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.version"),
                System.getProperty("os.arch"), Runtime.getRuntime().availableProcessors(), Locale.getDefault());
        // start the check for new version, making sure it is properly handled during execute
        final Scheduler scheduler = new Scheduler();
        App.SHUTDOWN_HOOKS.register(scheduler);
        App.SHUTDOWN_HOOKS.register(new VersionChecker());
        // read the command line and execute the runtime
        boolean faultTolerant = false;
        try {
            // prepare command line
            final Optional<CommandLineInterface> optionalCli = CommandLineInterface.parse(args);
            if (!optionalCli.isPresent()) {
                App.exit(ReturnCode.ERROR_WRONG_PARAMETERS);
            }
            final CommandLineInterface cli = optionalCli.get();
            // configure application
            faultTolerant = cli.isFaultTolerant();
            final Optional<AuthenticationHandler> optionalAuth = cli.newAuthenticationHandler();
            if (!optionalAuth.isPresent()) {
                App.exit(ReturnCode.ERROR_WRONG_PARAMETERS);
            }
            final AuthenticationHandler auth = optionalAuth.get();
            final Optional<Configuration> optionalCtx = cli.newApplicationConfiguration(auth.getSecretProvider());
            optionalCtx.ifPresent(ctx -> App.exit(App.execute(ctx, auth, scheduler))); // core investing algorithm
            App.exit(ReturnCode.ERROR_WRONG_PARAMETERS);
        } catch (final ProcessingException | NotAllowedException ex) {
            App.handleException(ex, faultTolerant);
        } catch (final WebApplicationException ex) {
            App.handleException(ex);
        } catch (final RuntimeException ex) {
            App.handleUnexpectedException(ex);
        }
    }

    static void handleException(final Exception ex, final boolean faultTolerant) {
        final Throwable cause = ex.getCause();
        if (ex instanceof NotAllowedException || cause instanceof SocketException ||
                cause instanceof UnknownHostException) {
            App.handleZonkyMaintenanceError(ex, faultTolerant);
        } else {
            App.handleUnexpectedException(ex);
        }
    }

    static void handleException(final WebApplicationException ex) {
        App.LOGGER.error("Application encountered remote API error.", ex);
        App.exit(ReturnCode.ERROR_REMOTE, ex);
    }

    static void handleUnexpectedException(final Exception ex) {
        App.LOGGER.error("Unexpected error, likely RoboZonky bug.", ex);
        App.exit(ReturnCode.ERROR_UNEXPECTED, ex);
    }

    static void handleZonkyMaintenanceError(final Exception ex, final boolean faultTolerant) {
        App.LOGGER.warn("Application not allowed to access remote API, Zonky likely down for maintenance.", ex);
        if (faultTolerant) {
            App.LOGGER.info("RoboZonky is in fault-tolerant mode. The above will not be reported as error.");
            App.exit(ReturnCode.OK, ex);
        } else {
            App.exit(ReturnCode.ERROR_DOWN, ex);
        }
    }

}
