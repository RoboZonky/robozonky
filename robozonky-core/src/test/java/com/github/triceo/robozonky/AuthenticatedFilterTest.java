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

package com.github.triceo.robozonky;

import java.io.IOException;
import java.util.UUID;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;

import com.github.triceo.robozonky.remote.Token;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class AuthenticatedFilterTest extends AbstractCommonFilterTest {

    private final Token token = Mockito.mock(Token.class);

    @Override
    protected CommonFilter getTestedFilter() {
        return new AuthenticatedFilter(token);
    }

    @Test
    public void hasToken() throws IOException {
        final String id = UUID.randomUUID().toString();
        Mockito.when(token.getAccessToken()).thenReturn(id);
        final ClientRequestContext crc = Mockito.mock(ClientRequestContext.class);
        Mockito.when(crc.getHeaders()).thenReturn(new MultivaluedHashMap<>());

        getTestedFilter().filter(crc);
        Assertions.assertThat(crc.getHeaders().getFirst("Authorization")).isEqualTo("Bearer " + token.getAccessToken());
    }

}
