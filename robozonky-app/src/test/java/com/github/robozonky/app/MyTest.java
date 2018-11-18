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

package com.github.robozonky.app;

import com.github.robozonky.app.authentication.TenantBuilder;
import com.github.robozonky.app.daemon.BlockedAmountProcessor;
import com.github.robozonky.app.daemon.Portfolio;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.secrets.SecretProvider;
import org.junit.jupiter.api.Test;

public class MyTest {

    @Test
    void testing() {
        final Tenant tenant = new TenantBuilder()
                .withSecrets(SecretProvider.inMemory("lukas@petrovicky.net", "eN+U97:%EpsN?*Av".toCharArray()))
                .build();
        final Portfolio p = Portfolio.create(tenant, BlockedAmountProcessor.createLazy(tenant));
        System.out.println(p.getOverview());
    }

}
