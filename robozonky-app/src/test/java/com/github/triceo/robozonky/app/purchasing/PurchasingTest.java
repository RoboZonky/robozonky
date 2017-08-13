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

package com.github.triceo.robozonky.app.purchasing;

import java.time.OffsetDateTime;

import com.github.triceo.robozonky.app.authentication.Authenticated;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class PurchasingTest {

    @Test
    public void update() {
        final Purchasing p = new Purchasing(Mockito.mock(Authenticated.class), null, null, true);
        Assertions.assertThat(p.getLastRunDateTime()).isNull();
        p.run();
        Assertions.assertThat(p.getLastRunDateTime()).isBeforeOrEqualTo(OffsetDateTime.now());
    }
}
