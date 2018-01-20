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

package com.github.robozonky.app.version;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.util.Refreshable;
import com.github.robozonky.util.Schedulers;
import org.apache.commons.io.IOUtils;

/**
 * Periodically queries remote Zonky API, checking whether it's accessible. Based on that, it will trigger
 * {@link RuntimeControl} to call either {@link Schedulers#pause()} or {@link Schedulers#resume()}, therefore
 * controlling the entire daemon runtime.
 */
public class LivenessCheck extends Refreshable<ApiVersion> {

    public static ShutdownHook.Handler setup() {
        Schedulers.INSTANCE.pause(); // don't run anything until Zonky is up and running
        final Refreshable<ApiVersion> liveness = new LivenessCheck();
        final Refreshable.RefreshListener<ApiVersion> listener = new RuntimeControl();
        liveness.registerListener(listener);
        // independent of the other schedulers; it controls whether or not the others are even allowed to run
        final ScheduledExecutorService e = Executors.newScheduledThreadPool(1);
        e.scheduleWithFixedDelay(liveness, 0, Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
        return () -> Optional.of(returnCode -> e.shutdownNow());
    }

    private static final String URL = ApiProvider.ZONKY_URL + "/version";

    private final String url;

    private LivenessCheck() {
        this(URL);
    }

    LivenessCheck(final String url) {
        this.url = url;
    }

    @Override
    protected Optional<String> getLatestSource() {
        try (final InputStream s = new URL(url).openStream()) {
            final String json = IOUtils.readLines(s, Defaults.CHARSET).stream()
                    .collect(Collectors.joining(System.lineSeparator()));
            return Optional.of(json);
        } catch (final MalformedURLException ex) {
            LOGGER.trace("Wrong Zonky URL.", ex);
        } catch (final IOException ex) {
            LOGGER.trace("Failed communicating with Zonky, likely down or cannot be reached.", ex);
        }
        return Optional.empty();
    }

    @Override
    protected Optional<ApiVersion> transform(final String source) {
        try {
            final ApiVersion version = ApiVersion.read(source);
            return Optional.of(version);
        } catch (final IOException ex) {
            LOGGER.warn("Failed parsing Zonky version info.", ex);
            return Optional.empty();
        }
    }
}
