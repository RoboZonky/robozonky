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

import com.github.triceo.robozonky.internal.api.AbstractApiProvider;
import com.github.triceo.robozonky.internal.api.RoboZonkyFilter;

class MarketplaceApiProvider extends AbstractApiProvider {

    private static final String ZOTIFY_URL = "http://zotify.cz";
    private static final RoboZonkyFilter FILTER = new RoboZonkyFilter();

    /**
     * Retrieve Zotify's marketplace cache.
     *
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    public ZotifyApi zotify() {
        return this.obtain(ZotifyApi.class, MarketplaceApiProvider.ZOTIFY_URL, MarketplaceApiProvider.FILTER);
    }

}
