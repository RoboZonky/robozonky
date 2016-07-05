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

package com.github.triceo.robozonky.operations;

import java.util.Optional;

import com.github.triceo.robozonky.remote.InvestingZonkyApi;
import com.github.triceo.robozonky.remote.Investment;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class InvestOperationTest {

    @Test
    public void properOperation() {
        final Investment investmentMock = Mockito.mock(Investment.class);
        final InvestingZonkyApi apiMock = Mockito.mock(InvestingZonkyApi.class);
        final Optional<Investment> optional = new InvestOperation().apply(apiMock, investmentMock);
        Assertions.assertThat(optional).isPresent();
        Assertions.assertThat(optional).hasValue(investmentMock);
    }

    @Test
    public void failedOperation() {
        final Investment investmentMock = Mockito.mock(Investment.class);
        final InvestingZonkyApi apiMock = Mockito.mock(InvestingZonkyApi.class);
        Mockito.doThrow(IllegalStateException.class).when(apiMock).invest(investmentMock);
        final Optional<Investment> optional = new InvestOperation().apply(apiMock, investmentMock);
        Assertions.assertThat(optional).isEmpty();
    }


}
