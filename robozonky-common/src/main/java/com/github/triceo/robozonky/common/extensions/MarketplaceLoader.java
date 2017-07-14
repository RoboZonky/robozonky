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

package com.github.triceo.robozonky.common.extensions;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.api.marketplaces.MarketplaceService;
import com.github.triceo.robozonky.common.secrets.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MarketplaceLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketplaceLoader.class);
    private static final ServiceLoader<MarketplaceService> LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(MarketplaceService.class);

    static Optional<Marketplace> processMarketplace(final MarketplaceService service, final Credentials credentials) {
        final String providerId = credentials.getToolId();
        MarketplaceLoader.LOGGER.debug("Evaluating marketplace '{}' with '{}'.", providerId, service.getClass());
        final char[] secret = credentials.getToken().orElse(new char[0]);
        return service.find(providerId, secret);
    }

    static Optional<Marketplace> load(final Credentials credentials, final Iterable<MarketplaceService> loader) {
        MarketplaceLoader.LOGGER.trace("Looking up marketplace '{}'.", credentials.getToolId());
        return Util.toStream(loader)
                .map(service -> MarketplaceLoader.processMarketplace(service, credentials))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .findFirst();
    }

    public static Optional<Marketplace> load(final Credentials credentials) {
        return MarketplaceLoader.load(credentials, MarketplaceLoader.LOADER);
    }

}

