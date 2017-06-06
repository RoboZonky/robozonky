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
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ApiTest {

    @Test
    public void lifecycle() {
        final ZonkyApiToken token = AuthenticatedFilterTest.TOKEN;
        final Apis apis = Mockito.spy(new Apis());
        final Api collection = Mockito.spy(new Api(apis, token));
        try (final Api c = collection) {
            // make sure mock have been created
            Mockito.verify(apis, Mockito.times(1)).loans(ArgumentMatchers.eq(token));
            Mockito.verify(apis, Mockito.times(1)).wallet(ArgumentMatchers.eq(token));
            Mockito.verify(apis, Mockito.times(1)).portfolio(ArgumentMatchers.eq(token));
            Mockito.verify(apis, Mockito.times(1)).control(ArgumentMatchers.eq(token));
            // make sure mock can be accessed
            collection.execute((u, v, w, x) -> {
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(u).isNotNull();
                    softly.assertThat(v).isNotNull();
                    softly.assertThat(w).isNotNull();
                    softly.assertThat(x).isNotNull();
                });
                return Void.TYPE;
            });
        }
        // execute after closing will throw
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> collection.execute((control, x, y, z) -> {
                control.logout();
                return Void.TYPE;
            })).isInstanceOf(IllegalStateException.class);
            softly.assertThatThrownBy(() -> collection.execute((x, loans, y, z) -> loans.items()))
                    .isInstanceOf(IllegalStateException.class);
            softly.assertThatThrownBy(() -> collection.execute((x, y, wallet, z) -> wallet.items()))
                    .isInstanceOf(IllegalStateException.class);
            softly.assertThatThrownBy(() -> collection.execute((x, y, z, portfolio) -> portfolio.items()))
                    .isInstanceOf(IllegalStateException.class);
        });
    }

}
