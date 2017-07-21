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

package com.github.triceo.robozonky.common.remote;

import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class ApiProviderTest {

    @Test
    public void unathenticatedApis() {
        final ApiProvider provider = new ApiProvider();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(provider.marketplace()).isNotNull();
            Assertions.assertThat(provider.oauth()).isNotNull();
        });
    }

    @Test
    public void athenticatedApis() {
        final ZonkyApiToken token = AuthenticatedFilterTest.TOKEN;
        final ApiProvider provider = new ApiProvider();
        Assertions.assertThat(provider.authenticated(token)).isNotNull();
    }
}
