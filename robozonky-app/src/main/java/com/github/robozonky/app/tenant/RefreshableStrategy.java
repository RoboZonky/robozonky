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

package com.github.robozonky.app.tenant;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;

import com.github.robozonky.common.async.Refreshable;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.util.UrlUtil;
import io.vavr.control.Try;
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
    protected String getLatestSource() {
        return Try.withResources(() -> UrlUtil.open(url))
                .of(s -> IOUtils.toString(s, Defaults.CHARSET))
                .getOrElseThrow((Function<Throwable, IllegalStateException>) IllegalStateException::new);
    }

    @Override
    protected Optional<String> transform(final String source) {
        return Optional.of(source);
    }
}
