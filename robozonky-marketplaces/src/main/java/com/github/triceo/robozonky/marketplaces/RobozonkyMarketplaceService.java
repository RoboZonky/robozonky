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

package com.github.triceo.robozonky.marketplaces;

import java.util.Objects;
import java.util.Optional;

import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.api.marketplaces.MarketplaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RobozonkyMarketplaceService implements MarketplaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RobozonkyMarketplaceService.class);

    @Override
    public Optional<Marketplace> find(final String marketplaceId, final char... secret) {
        RobozonkyMarketplaceService.LOGGER.debug("Ignoring secret, since core marketplaces requires none.");
        if (Objects.equals("zotify", marketplaceId)) {
            return Optional.of(new ZotifyMarketplace());
        } else if (Objects.equals("zonky", marketplaceId)) {
            return Optional.of(new ZonkyMarketplace());
        } else {
            return Optional.empty();
        }
    }
}
