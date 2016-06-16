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

package com.github.triceo.robozonky.authentication;

import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import com.github.triceo.robozonky.remote.ZotifyApi;

public class Authentication {

    private final ZonkyApi api;
    private final ZotifyApi zotifyApi;
    private final ZonkyApiToken apiToken;

    Authentication(final ZonkyApi api, final ZonkyApiToken apiToken, final ZotifyApi zotifyApi) {
        this.api = api;
        this.zotifyApi = zotifyApi;
        this.apiToken = apiToken;
    }

    public ZonkyApi getZonkyApi() {
        return api;
    }

    public ZotifyApi getZotifyApi() {
        return zotifyApi;
    }

    public ZonkyApiToken getZonkyApiToken() {
        return apiToken;
    }
}
