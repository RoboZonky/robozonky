/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.common;

import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantTest {

    @Test
    void defaultMethods() {
        final Tenant t = spy(Tenant.class);
        when(t.getSessionInfo()).thenReturn(new SessionInfo("someone@somewhere.cz"));
        t.isAvailable();
        verify(t).isAvailable(eq(ZonkyScope.getDefault()));
        final Function<Zonky, String> f = z -> "";
        t.call(f);
        verify(t).call(eq(f));
        final Consumer<Zonky> c = z -> {};
        t.run(c);
        verify(t).run(eq(c));
        assertThat(t.getState(TenantTest.class)).isNotNull();
    }

}
