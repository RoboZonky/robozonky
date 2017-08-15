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

package com.github.triceo.robozonky.app.authentication;

import java.util.function.Consumer;
import java.util.function.Function;

import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import org.junit.Test;
import org.mockito.Mockito;

public class AuthenticatedTest {

    @Test
    public void defaultMethod() {
        final Zonky z = Mockito.mock(Zonky.class);
        final Authenticated a = new Authenticated() {
            @Override
            public <T> T call(final Function<Zonky, T> operation) {
                return operation.apply(z);
            }

            @Override
            public SecretProvider getSecretProvider() {
                return null;
            }
        };
        final Consumer<Zonky> c = zonky -> z.logout();
        a.run(c);
        Mockito.verify(z).logout();
    }
}
