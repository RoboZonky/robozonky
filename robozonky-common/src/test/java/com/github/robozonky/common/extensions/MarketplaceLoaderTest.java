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

package com.github.robozonky.common.extensions;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.api.marketplaces.MarketplaceService;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.common.secrets.Credentials;
import com.github.robozonky.common.secrets.SecretProvider;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class MarketplaceLoaderTest {

    private static final SecretProvider SECRETS = Mockito.mock(SecretProvider.class);

    @Test
    public void loadNonexistent() {
        final Credentials c = new Credentials(UUID.randomUUID().toString(), MarketplaceLoaderTest.SECRETS);
        Assertions.assertThat(MarketplaceLoader.load(c)).isEmpty();
    }

    @Test
    public void processing() {
        final Credentials c = new Credentials(UUID.randomUUID().toString(), MarketplaceLoaderTest.SECRETS);
        Assertions.assertThat(MarketplaceLoader.processMarketplace(Mockito.mock(MarketplaceService.class), c))
                .isEmpty();
    }

    @Test
    public void loading() {
        final Marketplace m = new Marketplace() {
            @Override
            public boolean registerListener(final Consumer<Collection<Loan>> listener) {
                return false;
            }

            @Override
            public void run() {

            }
        };
        final MarketplaceService ms = (marketplaceId, secret) -> Optional.of(m);
        final Credentials c = new Credentials("", SecretProvider.fallback(""));
        Assertions.assertThat(MarketplaceLoader.load(c, Collections.singleton(ms))).contains(m);
    }
}
