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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.github.triceo.robozonky.api.remote.InvestingZonkyApi;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.ZotifyApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ApiProviderTest {

    private static final ZonkyApiToken TOKEN = new ZonkyApiToken(UUID.randomUUID().toString(),
            UUID.randomUUID().toString(), 300);


    @Parameterized.Parameters(name = "isDryRun={0} -> {1}")
    public static Collection<Object[]> getParameters() {
        final List<Object[]> result = new ArrayList<>(2);
        result.add(new Object[] {true, ZonkyApi.class});
        result.add(new Object[] {false, InvestingZonkyApi.class});
        return result;
    }

    @Parameterized.Parameter
    public boolean isDryRun;
    @Parameterized.Parameter(1)
    public Class<?> type;

    @Test
    public void obtainAndDestroy() {
        ZotifyApi cache = null;
        final SoftAssertions softly = new SoftAssertions();
        try (final ApiProvider provider = new ApiProvider(isDryRun)) {
            cache = provider.cache(); // make sure API was provided
            softly.assertThat(provider.authenticated(ApiProviderTest.TOKEN)).isInstanceOf(type);
            softly.assertThat(cache).isNotNull();
            softly.assertThat(cache.getLoans()).isNotEmpty();
        } finally { // make sure the client was closed
            softly.assertThatThrownBy(cache::getLoans).isNotNull();
        }
        softly.assertAll();
    }

}
