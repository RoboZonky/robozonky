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

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.notifications.RoboZonkyStartingEvent;
import com.github.triceo.robozonky.app.configuration.CommandLine;
import com.github.triceo.robozonky.app.investing.InvestmentMode;
import com.github.triceo.robozonky.app.version.UpdateMonitor;
import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * You are required to exit this app by calling {@link #exit(ReturnCode)}.
 */
public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    static final ShutdownHook SHUTDOWN_HOOKS = new ShutdownHook();

    static void exit(final ReturnCode returnCode) {
        App.LOGGER.trace("Exit requested with return code {}.", returnCode);
        App.exit(new ShutdownHook.Result(returnCode, null));
    }

    /**
     * Will terminate the application. Call this on every exit of the app to ensure proper termination. Failure to do
     * so may result in unpredictable behavior of this instance of RoboZonky or future ones.
     * @param result Will be passed to {@link System#exit(int)}.
     */
    static void exit(final ShutdownHook.Result result) {
        App.SHUTDOWN_HOOKS.execute(result);
        System.exit(result.getReturnCode().getCode());
    }

    static ReturnCode execute(final InvestmentMode mode) {
        try {
            return mode.get().map(r -> {
                App.LOGGER.info("RoboZonky {}invested into {} loans.", mode.isDryRun() ? "would have " : "", r.size());
                return ReturnCode.OK;
            }).orElse(ReturnCode.ERROR_SETUP);
        } finally {
            try {
                mode.close();
            } catch (final Exception ex) {
                App.LOGGER.debug("Failed cleaning up post investing.", ex);
            }
        }
    }

    static ReturnCode execute(final InvestmentMode mode, final AtomicBoolean faultTolerant) {
        App.SHUTDOWN_HOOKS.register(new ShutdownEnabler());
        App.SHUTDOWN_HOOKS.register(new Management());
        App.SHUTDOWN_HOOKS.register(new RoboZonkyStartupNotifier());
        faultTolerant.set(mode.isFaultTolerant());
        return App.execute(mode);
    }

    static ReturnCode execute(final String[] args, final AtomicBoolean faultTolerant) {
        return CommandLine.parse(args)
                .map(mode -> App.execute(mode, faultTolerant))
                .orElse(ReturnCode.ERROR_WRONG_PARAMETERS);
    }

    public static void main(final String... args) {
        App.LOGGER.debug("Current working directory is '{}'.", System.getProperty("user.dir"));
        Events.fire(new RoboZonkyStartingEvent(Defaults.ROBOZONKY_VERSION));
        App.LOGGER.debug("Running {} Java v{} on {} v{} ({}, {} CPUs, {}, {}).", System.getProperty("java.vendor"),
                System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.version"),
                System.getProperty("os.arch"), Runtime.getRuntime().availableProcessors(), Locale.getDefault(),
                Charset.defaultCharset());
        App.SHUTDOWN_HOOKS.register(() -> Optional.of(returnCode -> Scheduler.BACKGROUND_SCHEDULER.shutdown()));
        // check for new RoboZonky version every now and then
        Scheduler.BACKGROUND_SCHEDULER.submit(new UpdateMonitor(), Duration.ofHours(1));
        // read the command line and execute the runtime
        final AtomicBoolean faultTolerant = new AtomicBoolean(false);
        try { // execute core code
            App.exit(App.execute(args, faultTolerant));
        } catch (final Exception ex) {
            new AppRuntimeExceptionHandler(faultTolerant.get()).handle(ex);
        }
    }

}
