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

import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.BlockedAmounts;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.portfolio.PortfolioDependant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class BlockedAmountsUpdaterTest {

    @Test
    public void hasDependant() {
        final BlockedAmountsUpdater bau = new BlockedAmountsUpdater(Mockito.mock(Authenticated.class), Optional::empty);
        Assertions.assertThat(bau.getDependant()).isInstanceOf(BlockedAmounts.class);
    }

    @Test
    public void noDependant() {
        final BlockedAmountsUpdater bau = new BlockedAmountsUpdater(Mockito.mock(Authenticated.class), Optional::empty,
                                                                    null);
        bau.run(); // quietly ignored
    }

    @Test
    public void customDependant() {
        final Portfolio p = Mockito.mock(Portfolio.class);
        final PortfolioDependant d = Mockito.mock(PortfolioDependant.class);
        final Authenticated auth = Mockito.mock(Authenticated.class);
        final BlockedAmountsUpdater bau = new BlockedAmountsUpdater(auth, () -> Optional.of(p), d);
        bau.run();
        Mockito.verify(d).accept(ArgumentMatchers.eq(p), ArgumentMatchers.eq(auth));
    }
}

