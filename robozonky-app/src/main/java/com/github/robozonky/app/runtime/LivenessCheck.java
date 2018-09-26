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

package com.github.robozonky.app.runtime;

import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.util.IoUtil;
import com.github.robozonky.util.Refreshable;
import com.github.robozonky.util.RoboZonkyThreadFactory;
import com.github.robozonky.util.Schedulers;
import org.apache.commons.io.IOUtils;

/**
 * Periodically queries remote Zonky API, checking whether it's accessible. Based on that, it will trigger
 * {@link SchedulerControl} to call either {@link Schedulers#pause()} or {@link Schedulers#resume()}, therefore
 * controlling the entire daemon runtime.
 */
class LivenessCheck extends Refreshable<String> {

    private static final String ZONKY_VERSION_URL = ApiProvider.ZONKY_URL + "/version";

    private final String url;

    private LivenessCheck() {
        this(ZONKY_VERSION_URL);
    }

    LivenessCheck(final String url) {
        this.url = url;
    }

    private static ThreadFactory getThreadFactory() {
        final ThreadGroup tg = new ThreadGroup("rzLiveness");
        tg.setDaemon(true);
        return new RoboZonkyThreadFactory(tg);
    }

    public static ShutdownHook.Handler setup(final MainControl mainThreadControl) {
        final Refreshable<String> liveness = new LivenessCheck();
        final Refreshable.RefreshListener<String> schedulerControl = new SchedulerControl();
        liveness.registerListener(schedulerControl);
        liveness.registerListener(mainThreadControl);
        // independent of the other schedulers; it controls whether or not the others are even allowed to run
        final ScheduledExecutorService e = Executors.newScheduledThreadPool(1, getThreadFactory());
        e.scheduleWithFixedDelay(liveness, 0, Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
        return () -> Optional.of(returnCode -> e.shutdownNow());
    }

    @Override
    protected String getLatestSource() {
        try {
            return IoUtil.tryFunction(() -> new URL(url).openStream(), s -> {
                final String source = IOUtils.readLines(s, Defaults.CHARSET).stream()
                        .collect(Collectors.joining(System.lineSeparator()));
                LOGGER.trace("API info coming from Zonky: {}.", source);
                final ApiVersion version = ApiVersion.read(source);
                // need to send parsed version, since the object itself changes every time due to currentApiTime field
                return version.getBuildVersion();
            });
        } catch (final Exception ex) {
            // don't propagate this exception as it is likely to happen and the calling code would WARN about it
            LOGGER.debug("Zonky servers are likely unavailable.", ex);
            return null; // will fail during transform()
        }
    }

    @Override
    protected Optional<String> transform(final String source) {
        return Optional.ofNullable(source);
    }
}
