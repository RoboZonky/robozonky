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

package com.github.triceo.robozonky.api.marketplaces;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Represents a provider for Zonky loans, such as Zonky Marketplace, Zotify or Pushbullet, to be used via
 * {@link ServiceLoader}.
 */
public interface MarketplaceService {

    /**
     * Find a marketplace of a given ID that is able to serve loans using a given secret.
     * @param marketplaceId Type of the marketplace to serve, such as "zonky" or "zotify".
     * @param secret Whatever secret that the {@link Marketplace} needs to authenticate with the data source.
     * @return The instance to serve the loans, if one is found.
     */
    Optional<Marketplace> find(String marketplaceId, char[] secret);
}
