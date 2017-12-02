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

package com.github.robozonky.app.configuration.daemon;

import java.util.Optional;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class PortfolioUpdaterTest extends AbstractZonkyLeveragingTest {

    @Test
    public void updatingDependants() {
        final Zonky z = harmlessZonky(10_000);
        final Authenticated a = mockAuthentication(z);
        final PortfolioDependant dependant = Mockito.mock(PortfolioDependant.class);
        final PortfolioUpdater instance = new PortfolioUpdater(a);
        Assertions.assertThat(instance.isUpdating()).isFalse();
        instance.registerDependant(dependant);
        instance.run();
        Mockito.verify(a).call(ArgumentMatchers.any()); // this is the call to update Portfolio
        final Optional<Portfolio> result = instance.get();
        // make sure that the dependants were called with the proper value of Portfolio
        Mockito.verify(dependant).accept(ArgumentMatchers.eq(result.get()), ArgumentMatchers.eq(a));
        Assertions.assertThat(instance.isUpdating()).isFalse();
    }


}
