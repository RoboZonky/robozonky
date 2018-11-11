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

import com.github.robozonky.app.configuration.CommandLine;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.events.EventFactory;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.app.management.Management;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.util.IoUtil;
import com.github.robozonky.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * You are required to exit this app by calling {@link #exit(ReturnCode)}.
 */
public class App implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private final ShutdownHook shutdownHooks = new ShutdownHook();
    private final Lifecycle lifecycle = new Lifecycle(shutdownHooks);
    private final String[] args;

    public App(final String... args) {
        this.args = args.clone();
    }

    public static void main(final String... args) {
        final App main = new App(args);
        main.run();
    }

    private void exit(final ReturnCode returnCode) {
        LOGGER.trace("Exit requested with return code {}.", returnCode);
        final ShutdownHook.Result r = lifecycle.getTerminationCause()
                .map(t -> new ShutdownHook.Result(ReturnCode.ERROR_UNEXPECTED, t))
                .orElse(new ShutdownHook.Result(returnCode, null));
        exit(r);
    }

    /**
     * Exists so that tests can mock the {@link System#exit(int)} away.
     * @param code
     */
    public void actuallyExit(final int code) {
        System.exit(code);
    }

    /**
     * Will terminate the application. Call this on every exit of the app to ensure proper termination. Failure to do
     * so may result in unpredictable behavior of this instance of RoboZonky or future ones.
     * @param result Will be passed to {@link System#exit(int)}.
     */
    public void exit(final ShutdownHook.Result result) {
        shutdownHooks.execute(result);
        actuallyExit(result.getReturnCode().getCode());
    }

    ReturnCode execute(final InvestmentMode mode) {
        try {
            return IoUtil.tryFunction(() -> mode, this::executeSafe);
        } catch (final Throwable t) {
            LOGGER.error("Caught unexpected exception, terminating daemon.", t);
            return ReturnCode.ERROR_UNEXPECTED;
        }
    }

    private ReturnCode executeSafe(final InvestmentMode m) {
        shutdownHooks.register(() -> Optional.of(r -> Scheduler.inBackground().close()));
        Events.allSessions().fire(EventFactory.roboZonkyStarting());
        ensureLiveness();
        shutdownHooks.register(new Management(lifecycle));
        shutdownHooks.register(new RoboZonkyStartupNotifier(m.getSessionName()));
        return m.apply(lifecycle);
    }

    public void resumeToFail(final Throwable throwable) {
        lifecycle.resumeToFail(throwable);
    }

    void ensureLiveness() {
        if (!lifecycle.waitUntilOnline()) {
            exit(ReturnCode.ERROR_DOWN);
        }
    }

    public String[] getArgs() {
        return args.clone();
    }

    @Override
    public void run() {
        LOGGER.debug("Current working directory is '{}'.", System.getProperty("user.dir"));
        LOGGER.debug("Running {} {} v{} on {} v{} ({}, {} CPUs, {}, {}).", System.getProperty("java.vm.vendor"),
                     System.getProperty("java.vm.name"), System.getProperty("java.vm.version"),
                     System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"),
                     Runtime.getRuntime().availableProcessors(), Locale.getDefault(), Charset.defaultCharset());
        final ReturnCode code = CommandLine.parse(this).map(this::execute).orElse(ReturnCode.ERROR_SETUP);
        exit(code); // call the core code
    }
}
