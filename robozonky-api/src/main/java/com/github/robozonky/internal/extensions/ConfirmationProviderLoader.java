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

package com.github.robozonky.internal.extensions;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.confirmations.ConfirmationProviderService;
import com.github.robozonky.internal.util.StreamUtil;
import io.vavr.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ConfirmationProviderLoader {

    private static final Logger LOGGER = LogManager.getLogger(ConfirmationProviderLoader.class);
    private static final Lazy<ServiceLoader<ConfirmationProviderService>> LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(ConfirmationProviderService.class);

    private ConfirmationProviderLoader() {
        // no instances
    }

    static Optional<ConfirmationProvider> load(final String providerId,
                                               final Iterable<ConfirmationProviderService> loader) {
        LOGGER.debug("Looking up confirmation provider '{}'.", providerId);
        return StreamUtil.toStream(loader)
                .peek(cp -> LOGGER.trace("Evaluating confirmation provider '{}' with '{}'.", providerId, cp.getClass()))
                .map(cp -> cp.find(providerId))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .findFirst();
    }

    public static Optional<ConfirmationProvider> load(final String providerId) {
        return load(providerId, LOADER.get());
    }
}

