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

package com.github.robozonky.app;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Optional;

import com.github.robozonky.api.remote.enums.InvestmentType;
import com.github.robozonky.app.configuration.CommandLine;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.internal.api.async.Tasks;
import com.github.robozonky.internal.api.management.Management;
import com.github.robozonky.internal.util.BootstrapUtil;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * You are required to exit this app by calling {@link #exit(ReturnCode)}.
 */
public class App implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(App.class);

    private final ShutdownHook shutdownHooks = new ShutdownHook();
    private final Lazy<Lifecycle> lifecycle = Lazy.of(() -> new Lifecycle(shutdownHooks));
    private final String[] args;

    public App(final String... args) {
        this.args = args.clone();
    }

    public static void main(final String... args) {
        BootstrapUtil.configureLogging();
        final App main = new App(args);
        main.run();
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
    public void exit(final ReturnCode result) {
        LOGGER.trace("Exit requested with return code {}.", result);
        actuallyExit(result.getCode());
    }

    ReturnCode execute(final InvestmentMode mode) {
        return Try.withResources(() -> mode)
                .of(this::executeSafe)
                .getOrElseGet(t -> {
                    LOGGER.error("Caught unexpected exception, terminating daemon.", t);
                    return ReturnCode.ERROR_UNEXPECTED;
                });
    }

    private ReturnCode executeSafe(final InvestmentMode m) {
        Events.global().fire(EventFactory.roboZonkyStarting());
        ensureLiveness();
        final InvestmentType type = m.getInvestmentType();
        if (type != null && type != InvestmentType.N && type != InvestmentType.INVESTOR) {
            throw new IllegalStateException("Zonky Rentier customers can not use RoboZonky.");
        }
        // will close event-firing thread == must be triggered last == must be registered first
        shutdownHooks.register(() -> Optional.of(r -> Tasks.closeAll()));
        // will trigger events, therefore needs to come before the above
        shutdownHooks.register(new RoboZonkyStartupNotifier(m.getSessionInfo()));
        shutdownHooks.register(() -> Optional.of(result -> Management.unregisterAll()));
        // trigger all shutdown hooks in reverse order, before the token is closed after exiting this method
        try {
            final ReturnCode code = m.apply(getLifecycle());
            shutdownHooks.execute(code);
            return code;
        } catch (final Throwable ex) {
            shutdownHooks.execute(ReturnCode.ERROR_UNEXPECTED); // make sure all is closed even in a failing situation
            throw new IllegalStateException("Error executing daemon.", ex);
        }
    }

    public Lifecycle getLifecycle() {
        return lifecycle.get();
    }

    void ensureLiveness() {
        if (!getLifecycle().waitUntilOnline()) {
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
