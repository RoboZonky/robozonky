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

package com.github.triceo.robozonky.common.extensions;

import java.util.UUID;

import com.github.triceo.robozonky.common.secrets.Credentials;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
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
}
