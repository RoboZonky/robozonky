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

package com.github.triceo.robozonky.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.internal.api.Defaults;

/**
 * Will retrieve external IP address of a computer running the application by querying a remote service.
 */
public class LocalhostAddress extends Refreshable<String> {

    public static final Refreshable<String> INSTANCE = new LocalhostAddress();

    private LocalhostAddress() { // don't allow externally managed instances
        Scheduler.BACKGROUND_SCHEDULER.submit(this);
    }

    @Override
    protected Supplier<Optional<String>> getLatestSource() {
        final String url = "http://checkip.amazonaws.com";
        try (final BufferedReader in =
                     new BufferedReader(new InputStreamReader(new URL(url).openStream(), Defaults.CHARSET))) {
            final String s = in.readLine();
            return () -> Optional.of(s);
        } catch (final Exception ex) {
            LOGGER.debug("Failed retrieving local host address.", ex);
            return () -> Optional.of("localhost");
        }
    }

    @Override
    protected Optional<String> transform(final String source) {
        return Optional.of(source);
    }

}
