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

package com.github.triceo.robozonky.app.configuration;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Refreshable<T> implements Runnable {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final AtomicReference<String> latestSource = new AtomicReference<>();
    private final AtomicReference<T> latestResult = new AtomicReference<>();

    protected abstract Optional<String> getLatestSource();

    protected abstract Optional<T> transform(String source);

    public Optional<T> getLatest() {
        return Optional.ofNullable(latestResult.get());
    }

    @Override
    public void run() {
        getLatestSource().ifPresent(source -> {
            LOGGER.debug("New source found.");
            if (Objects.equals(source, latestSource.get())) {
                return;
            }
            // source changed, result needs to be refreshed
            latestSource.set(source);
            transform(source).ifPresent(strategy -> {
                LOGGER.debug("Source successfully transformed to {}.", strategy.getClass());
                latestResult.set(strategy);
            });
        });
    }

}
