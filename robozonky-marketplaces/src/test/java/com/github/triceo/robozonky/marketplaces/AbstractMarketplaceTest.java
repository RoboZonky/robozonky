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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.marketplaces.ExpectedTreatment;
import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class AbstractMarketplaceTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getMarketplaces() {
        return Arrays.asList(new Object[] {ZotifyMarketplace.class}, new Object[] {ZonkyMarketplace.class});
    }

    @Parameterized.Parameter
    public Class<? extends Marketplace> marketClass;

    @Test
    public void retrieval() throws Exception {
        final Consumer<Collection<Loan>> consumer = Mockito.mock(Consumer.class);
        try (final Marketplace market = marketClass.newInstance()) {
            Mockito.verify(consumer, Mockito.never()).accept(ArgumentMatchers.any());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(market.specifyExpectedTreatment()).isEqualTo(ExpectedTreatment.POLLING);
                softly.assertThat(market.registerListener(consumer)).isTrue();
            });
            market.run();
            Mockito.verify(consumer, Mockito.times(1))
                    .accept(ArgumentMatchers.argThat(argument -> argument != null && !argument.isEmpty()));
        }
    }

}
