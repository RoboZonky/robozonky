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

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProviderService;
import com.github.triceo.robozonky.app.util.ExtensionsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConfirmationProviderLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationProviderLoader.class);
    private static final ServiceLoader<ConfirmationProviderService> LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(ConfirmationProviderService.class);

    static Optional<ConfirmationProvider> load(final String providerId) {
        ConfirmationProviderLoader.LOGGER.trace("Looking up confirmation provider '{}'.", providerId);
        return StreamSupport.stream(ConfirmationProviderLoader.LOADER.spliterator(), false)
                .peek(cp ->
                        ConfirmationProviderLoader.LOGGER.debug("Evaluating confirmation provider '{}' with '{}'.",
                                providerId, cp.getClass()))
                .map(cp -> cp.find(providerId))
                .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())
                .findFirst();
    }

}

