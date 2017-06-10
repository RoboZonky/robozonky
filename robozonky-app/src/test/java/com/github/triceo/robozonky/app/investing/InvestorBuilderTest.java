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

package com.github.triceo.robozonky.app.investing;

import com.github.triceo.robozonky.common.remote.Zonky;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class InvestorBuilderTest {

    @Test
    public void build() {
        final String username = "user";
        final Investor.Builder b = new Investor.Builder();
        b.asUser(username).asDryRun();
        final Investor p = b.build(Mockito.mock(Zonky.class));
        SoftAssertions.assertSoftly(softly -> {
          softly.assertThat(p.getUsername()).isEqualTo(username);
          softly.assertThat(p.isDryRun()).isTrue();
        });
    }

}
