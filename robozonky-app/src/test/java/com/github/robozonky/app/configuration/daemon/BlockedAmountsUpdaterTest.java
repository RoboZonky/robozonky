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
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class BlockedAmountsUpdaterTest {

    @Test
    public void hasDependant() {
        final BlockedAmountsUpdater bau = new BlockedAmountsUpdater(Mockito.mock(Authenticated.class), Optional::empty);
        Assertions.assertThat(bau.getDependant()).isInstanceOf(BlockedAmounts.class);
    }
}
