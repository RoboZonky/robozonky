/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.common.remote;

import com.github.robozonky.api.remote.LoanApi;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProxyFactoryTest {

    @Test
    void api() {
        final ResteasyClient client = ProxyFactory.newResteasyClient();
        final RoboZonkyFilter f = new RoboZonkyFilter();
        assertThat(ProxyFactory.newProxy(client, f, LoanApi.class, "https://api.zonky.cz")).isNotNull();
    }

    @Test
    void unfilteredApi() {
        final ResteasyClient client = ProxyFactory.newResteasyClient();
        assertThat(ProxyFactory.newProxy(client, LoanApi.class, "https://api.zonky.cz")).isNotNull();
    }
}
