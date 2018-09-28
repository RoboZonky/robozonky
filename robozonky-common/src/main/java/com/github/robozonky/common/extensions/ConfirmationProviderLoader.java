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

package com.github.robozonky.common.extensions;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.confirmations.ConfirmationProviderService;
import com.github.robozonky.internal.util.LazyInitialized;
import com.github.robozonky.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfirmationProviderLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationProviderLoader.class);
    private static final LazyInitialized<ServiceLoader<ConfirmationProviderService>> LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(ConfirmationProviderService.class);

    private ConfirmationProviderLoader() {
        // no instances
    }

    static Optional<ConfirmationProvider> load(final String providerId,
                                               final Iterable<ConfirmationProviderService> loader) {
        ConfirmationProviderLoader.LOGGER.debug("Looking up confirmation provider '{}'.", providerId);
        return StreamUtil.toStream(loader)
                .peek(cp -> ConfirmationProviderLoader.LOGGER.trace("Evaluating confirmation provider '{}' with '{}'.",
                                                                    providerId, cp.getClass()))
                .map(cp -> cp.find(providerId))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .findFirst();
    }

    public static Optional<ConfirmationProvider> load(final String providerId) {
        return ConfirmationProviderLoader.load(providerId, ConfirmationProviderLoader.LOADER.get());
    }
}

