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

package com.github.robozonky.app.portfolio;

import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.authentication.TenantBuilder;
import com.github.robozonky.app.configuration.daemon.TransactionalPortfolio;
import com.github.robozonky.common.secrets.SecretProvider;
import org.junit.jupiter.api.Test;

class TransactionsTest {

    @Test
    void test() {
        final SecretProvider secrets = SecretProvider.inMemory("lukas@petrovicky.net",
                                                               "eN+U97:%EpsN?*Av".toCharArray());
        final Tenant tenant = new TenantBuilder().withSecrets(secrets).build();
        final Portfolio portfolio = Portfolio.create(tenant, RemoteBalance.create(tenant));
        final TransactionalPortfolio transactionalPortfolio = new TransactionalPortfolio(portfolio, tenant);
        final Carrier c = Carrier.create(transactionalPortfolio);
        final PortfolioOverview overview = c.getPortfolioOverview(tenant, portfolio.getRemoteBalance().get(),
                                                                  portfolio.getStatistics());
    }
}
