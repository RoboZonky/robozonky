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

package com.github.robozonky.notifications;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.robozonky.common.async.Refreshable;
import com.github.robozonky.internal.util.UrlUtil;
import io.vavr.control.Try;

import static com.github.robozonky.internal.api.Defaults.CHARSET;

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

    @Override
    protected Optional<ConfigStorage> transform(final String source) {
        return Try.withResources(() -> new ByteArrayInputStream(source.getBytes(CHARSET)))
                .of(baos -> Optional.of(ConfigStorage.create(baos)))
                .getOrElseGet(ex -> {
                    logger.warn("Failed transforming source.", ex);
                    return Optional.empty();
                });
    }

    @Override
    protected String getLatestSource() {
        logger.debug("Reading notification configuration from '{}'.", source);
        return Try.withResources(() -> new BufferedReader(new InputStreamReader(UrlUtil.open(source), CHARSET)))
                .of(r -> r.lines().collect(Collectors.joining(System.lineSeparator())))
                .getOrElseGet(t -> {
                    logger.warn("Failed reading notification configuration from '{}' due to '{}'.", source,
                                t.getMessage());
                    return null;
                });
    }
}
