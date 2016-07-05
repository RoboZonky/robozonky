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

import com.github.triceo.robozonky.remote.ZonkyApi;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class LogoutOperationTest {

    @Test
    public void properLogout() {
        final ZonkyApi apiMock = Mockito.mock(ZonkyApi.class);
        final Optional<Boolean> optional = new LogoutOperation().apply(apiMock);
        Assertions.assertThat(optional).isPresent();
    }

    @Test
    public void failedLogout() {
        final ZonkyApi apiMock = Mockito.mock(ZonkyApi.class);
        Mockito.doThrow(IllegalStateException.class).when(apiMock).logout();
        final Optional<Boolean> optional = new LogoutOperation().apply(apiMock);
        Assertions.assertThat(optional).isEmpty();
    }


}
