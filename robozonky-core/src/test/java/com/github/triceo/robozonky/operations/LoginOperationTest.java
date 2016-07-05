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

import com.github.triceo.robozonky.authentication.Authentication;
import com.github.triceo.robozonky.authentication.Authenticator;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class LoginOperationTest {

    @Test
    public void properLogin() {
        final Authentication tmp = Mockito.mock(Authentication.class);
        Mockito.when(tmp.getZonkyApi()).thenReturn(Mockito.mock(ZonkyApi.class));
        Mockito.when(tmp.getZonkyApiToken()).thenReturn(Mockito.mock(ZonkyApiToken.class));
        final Authenticator auth = Mockito.mock(Authenticator.class);
        Mockito.when(auth.authenticate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(tmp);
        final Optional<Authentication> optional = new LoginOperation().apply(auth);
        Assertions.assertThat(optional).isPresent();
        final Authentication c = optional.get();
        Assertions.assertThat(c.getZonkyApiToken()).isSameAs(tmp.getZonkyApiToken());
    }

    @Test
    public void failedLogin() {
        final Authenticator auth = Mockito.mock(Authenticator.class);
        Mockito.when(auth.authenticate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new IllegalStateException("Something bad happened."));
        Optional<Authentication> result = new LoginOperation().apply(auth);
        Assertions.assertThat(result).isEmpty();
    }


}
