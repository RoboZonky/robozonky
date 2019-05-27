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

package com.github.robozonky.internal.extensions;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.confirmations.ConfirmationProviderService;
import com.github.robozonky.api.confirmations.RequestId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConfirmationProviderLoaderTest {

    @Test
    void unknown() {
        final Optional<ConfirmationProvider> result = ConfirmationProviderLoader.load(UUID.randomUUID().toString());
        assertThat(result).isEmpty();
    }

    @Test
    void loading() {
        final String id = UUID.randomUUID().toString();
        final ConfirmationProvider cp = new ConfirmationProvider() {
            @Override
            public boolean requestConfirmation(final RequestId auth, final int loanId, final int amount) {
                return false;
            }

            @Override
            public String getId() {
                return id;
            }
        };
        final ConfirmationProviderService cps = strategy -> Optional.of(cp);
        assertThat(ConfirmationProviderLoader.load(id, Collections.singleton(cps))).contains(cp);
    }
}
