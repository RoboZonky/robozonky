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

package com.github.triceo.robozonky;

import com.github.triceo.robozonky.remote.ZotifyApi;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ApiProviderTest {

    @Test
    public void obtainAndDestroy() {
        ZotifyApi cache = null;
        try (final ApiProvider provider = new ApiProvider()) {
            cache = provider.cache(); // make sure API was provided
            Assertions.assertThat(cache).isNotNull();
            Assertions.assertThat(cache.getLoans()).isNotEmpty();
        } finally { // make sure the client was closed
            Assertions.assertThatThrownBy(cache::getLoans).isNotNull();
        }
    }

}
