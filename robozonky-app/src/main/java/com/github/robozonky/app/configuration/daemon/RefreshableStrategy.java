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

package com.github.robozonky.app.configuration.daemon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.internal.api.Defaults;
import org.apache.commons.io.IOUtils;

class RefreshableStrategy extends Refreshable<String> {

    private final URL url;

    protected RefreshableStrategy(final String target) {
        this(convertToUrl(target));
    }

    private RefreshableStrategy(final URL target) {
        this.url = target;
    }

    protected static URL convertToUrl(final String maybeUrl) {
        try {
            return new URL(maybeUrl);
        } catch (final MalformedURLException e) {
            try {
                return new File(maybeUrl).toURI().toURL();
            } catch (final NullPointerException | MalformedURLException e1) {
                throw new IllegalStateException("Cannot load strategy " + maybeUrl, e1);
            }
        }
    }

    @Override
    protected Optional<String> getLatestSource() {
        try (final InputStream s = url.openStream()) {
            return Optional.of(IOUtils.toString(s, Defaults.CHARSET));
        } catch (final IOException ex) {
            LOGGER.warn("Failed reading strategy.", ex);
            return Optional.empty();
        }
    }

    @Override
    protected Optional<String> transform(final String source) {
        return Optional.of(source);
    }
}
