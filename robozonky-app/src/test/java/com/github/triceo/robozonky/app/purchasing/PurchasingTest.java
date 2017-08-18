/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.triceo.robozonky.app.purchasing;

import java.time.OffsetDateTime;

import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.portfolio.Portfolio;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class PurchasingTest {

    @Before
    public void initPortfolio() {
        final Zonky z = Mockito.mock(Zonky.class);
        Portfolio.INSTANCE.update(z);
    }

    @Test
    public void update() {
        final Authenticated a = Mockito.mock(Authenticated.class);
        final Purchasing p = new Purchasing(a, null, null, true);
        Assertions.assertThat(p.getLastRunDateTime()).isNull();
        p.run();
        Mockito.verify(a).run(ArgumentMatchers.notNull());
        Assertions.assertThat(p.getLastRunDateTime()).isBeforeOrEqualTo(OffsetDateTime.now());
    }
}
