/*
 * Copyright 2018 The RoboZonky Project
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

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Optional;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.RoboZonkyStartingEvent;
import com.github.robozonky.app.configuration.CommandLine;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.management.Management;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.util.Scheduler;
import com.github.robozonky.util.SystemExitService;
import com.github.robozonky.util.SystemExitServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * You are required to exit this app by calling {@link #exit(ReturnCode)}.
 */
public class App {

    private static final ShutdownHook SHUTDOWN_HOOKS = new ShutdownHook();
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static final Lifecycle LIFECYCLE = new Lifecycle();

    private static void exit(final ReturnCode returnCode) {
        App.LOGGER.trace("Exit requested with return code {}.", returnCode);
        final ShutdownHook.Result r = LIFECYCLE.getTerminationCause()
                .map(t -> new ShutdownHook.Result(ReturnCode.ERROR_UNEXPECTED, t))
                .orElse(new ShutdownHook.Result(returnCode, null));
        App.exit(r);
    }

    /**
     * Will terminate the application. Call this on every exit of the app to ensure proper termination. Failure to do
     * so may result in unpredictable behavior of this instance of RoboZonky or future ones.
     * @param result Will be passed to {@link System#exit(int)}.
     */
    public static void exit(final ShutdownHook.Result result) {
        App.SHUTDOWN_HOOKS.execute(result);
        final SystemExitService exit = SystemExitServiceLoader.load();
        LOGGER.trace("System exit service received: {}.", exit);
        exit.newSystemExit().call(result.getReturnCode().getCode());
    }

    private static ReturnCode execute(final InvestmentMode mode) {
        App.SHUTDOWN_HOOKS.register(() -> Optional.of((r) -> Scheduler.inBackground().close()));
        Events.fire(new RoboZonkyStartingEvent());
        try {
            ensureLiveness();
            App.SHUTDOWN_HOOKS.register(new Management(LIFECYCLE));
            final String sessionName = Events.getSessionInfo().flatMap(SessionInfo::getName).orElse(null);
            App.SHUTDOWN_HOOKS.register(new RoboZonkyStartupNotifier(sessionName));
            return mode.apply(LIFECYCLE);
        } catch (final Throwable t) {
            LOGGER.error("Caught unexpected exception, terminating daemon.", t);
            return ReturnCode.ERROR_UNEXPECTED;
        }
    }

    private static void ensureLiveness() {
        App.LIFECYCLE.getShutdownHooks().forEach(App.SHUTDOWN_HOOKS::register);
        if (!App.LIFECYCLE.waitUntilOnline()) {
            App.exit(ReturnCode.ERROR_DOWN);
        }
    }

    public static void main(final String... args) {
        App.LOGGER.debug("Current working directory is '{}'.", System.getProperty("user.dir"));
        App.LOGGER.debug("Running {} {} v{} on {} v{} ({}, {} CPUs, {}, {}).", System.getProperty("java.vm.vendor"),
                         System.getProperty("java.vm.name"), System.getProperty("java.vm.version"),
                         System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"),
                         Runtime.getRuntime().availableProcessors(), Locale.getDefault(), Charset.defaultCharset());
        final ReturnCode code = configure(args).map(App::execute).orElse(ReturnCode.ERROR_SETUP);
        App.exit(code); // call the core code
    }

    private static Optional<InvestmentMode> configure(final String... args) {
        return CommandLine.parse(LIFECYCLE::resumeToFail, args);
    }
}
