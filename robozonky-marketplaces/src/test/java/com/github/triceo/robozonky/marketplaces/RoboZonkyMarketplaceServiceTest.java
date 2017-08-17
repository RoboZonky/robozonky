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

package com.github.triceo.robozonky.marketplaces;

import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RoboZonkyMarketplaceServiceTest {

    @Test
    public void zotifyRetrieval() {
        Assertions.assertThat(new RobozonkyMarketplaceService().find("zotify"))
                .isPresent().containsInstanceOf(ZotifyMarketplace.class);
    }

    @Test
    public void zonkyRetrieval() {
        Assertions.assertThat(new RobozonkyMarketplaceService().find("zonky"))
                .isPresent().containsInstanceOf(ZonkyMarketplace.class);
    }

    @Test
    public void nonexistent() {
        Assertions.assertThat(new RobozonkyMarketplaceService().find(UUID.randomUUID().toString())).isEmpty();
    }
}
