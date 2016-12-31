/*
 * Copyright 2016 Lukáš Petrovický
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

import com.github.triceo.robozonky.ExtensionsManager;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfirmationProviderLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationProviderLoader.class);
    private static final ServiceLoader<ConfirmationProviderService> LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(ConfirmationProviderService.class);

    static Optional<ConfirmationProvider> load(final String providerId) {
        ConfirmationProviderLoader.LOGGER.trace("Looking up confirmation provider '{}'.", providerId);
        for (final ConfirmationProviderService s : ConfirmationProviderLoader.LOADER) {
            final Optional<ConfirmationProvider> c = s.find(providerId);
            if (c.isPresent()) {
                ConfirmationProviderLoader.LOGGER.debug("Confirmation provider '{}' using {}.", providerId,
                        c.get().getClass());
                return c;
            } else {
                ConfirmationProviderLoader.LOGGER.trace("Confirmation provider '{}' not using {}.", providerId,
                        c.getClass());
            }
        }
        ConfirmationProviderLoader.LOGGER.trace("Finished looking up confirmation provider '{}'.", providerId);
        return Optional.empty();
    }

}

