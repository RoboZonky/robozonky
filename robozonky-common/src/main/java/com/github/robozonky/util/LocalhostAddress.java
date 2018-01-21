/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.util;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import com.github.robozonky.internal.api.Defaults;
import org.apache.commons.io.IOUtils;

/**
 * Will retrieve external IP address of a computer running the application by querying a remote service.
 */
public class LocalhostAddress extends Refreshable<String> {

    public static final Refreshable<String> INSTANCE = new LocalhostAddress();
    private static final String CHECKIP_URL = "http://checkip.amazonaws.com";

    LocalhostAddress() { // don't allow externally managed instances
        Scheduler.inBackground().submit(this);
    }

    @Override
    protected String getLatestSource() {
        try {
            final URL url = new URL(CHECKIP_URL);
            return IOUtils.toString(url, Defaults.CHARSET).trim();
        } catch (final IOException ex) {
            LOGGER.debug("Failed retrieving local host address.", ex);
            return "localhost";
        }
    }

    @Override
    protected Optional<String> transform(final String source) {
        return Optional.of(source);
    }
}
