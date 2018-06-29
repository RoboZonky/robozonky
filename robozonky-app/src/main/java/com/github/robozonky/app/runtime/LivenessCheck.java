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

import java.io.InputStream;
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
import com.github.robozonky.util.Refreshable;
import com.github.robozonky.util.RoboZonkyThreadFactory;
import com.github.robozonky.util.Schedulers;
import org.apache.commons.io.IOUtils;

/**
 * Periodically queries remote Zonky API, checking whether it's accessible. Based on that, it will trigger
 * {@link SchedulerControl} to call either {@link Schedulers#pause()} or {@link Schedulers#resume()}, therefore
 * controlling the entire daemon runtime.
 */
class LivenessCheck extends Refreshable<ApiVersion> {

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

    public static ShutdownHook.Handler setup(final MainControl livenessTrigger) {
        final Refreshable<ApiVersion> liveness = new LivenessCheck();
        final Refreshable.RefreshListener<ApiVersion> listener = new SchedulerControl();
        liveness.registerListener(listener);
        liveness.registerListener(livenessTrigger);
        // independent of the other schedulers; it controls whether or not the others are even allowed to run
        final ScheduledExecutorService e = Executors.newScheduledThreadPool(1, getThreadFactory());
        e.scheduleWithFixedDelay(liveness, 0, Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
        return () -> Optional.of(returnCode -> e.shutdownNow());
    }

    @Override
    protected String getLatestSource() {
        try (final InputStream s = new URL(url).openStream()) {
            return IOUtils.readLines(s, Defaults.CHARSET).stream().collect(Collectors.joining(System.lineSeparator()));
        } catch (final Exception ex) {
            // don't propagate this exception as it is likely to happen and the calling code would WARN about it
            LOGGER.debug("Zonky servers are likely unavailable.", ex);
            return ""; // will fail during transform()
        }
    }

    @Override
    protected Optional<ApiVersion> transform(final String source) {
        try {
            final ApiVersion version = ApiVersion.read(source);
            return Optional.of(version);
        } catch (final Exception ex) {
            LOGGER.warn("Failed parsing Zonky version info.", ex);
            return Optional.empty();
        }
    }
}
