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

package com.github.robozonky.notifications;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.util.IoUtil;
import com.github.robozonky.util.Refreshable;

public final class RefreshableConfigStorage extends Refreshable<ConfigStorage> {

    private final URL source;

    public RefreshableConfigStorage(final URL source) {
        this.source = source;
        /*
         * force the code to have a value right away. this is done to ensure that even the event listeners initialized
         * immediately after this call have notification properties available - otherwise initial emails of the platform
         * wouldn't have been sent until this Refreshable has had time to initialize. this has been a problem in the
         * installer already, as evidenced by https://github.com/RoboZonky/robozonky/issues/216.
         */
        run();
    }

    private static String readUrl(final URL url) throws IOException {
        return IoUtil.tryFunction(() -> new BufferedReader(new InputStreamReader(url.openStream(), Defaults.CHARSET)),
                                  r -> r.lines().collect(Collectors.joining(System.lineSeparator())));
    }

    @Override
    protected Optional<ConfigStorage> transform(final String source) {
        try {
            return IoUtil.tryFunction(() -> new ByteArrayInputStream(source.getBytes(Defaults.CHARSET)),
                                      baos -> Optional.of(ConfigStorage.create(baos)));
        } catch (final IOException ex) {
            LOGGER.warn("Failed transforming source.", ex);
            return Optional.empty();
        }
    }

    @Override
    protected String getLatestSource() {
        LOGGER.debug("Reading notification configuration from '{}'.", source);
        try {
            return RefreshableConfigStorage.readUrl(source);
        } catch (final IOException ex) {
            LOGGER.warn("Failed reading notification configuration from '{}' due to '{}'.", source, ex.getMessage());
            return null;
        }
    }
}
