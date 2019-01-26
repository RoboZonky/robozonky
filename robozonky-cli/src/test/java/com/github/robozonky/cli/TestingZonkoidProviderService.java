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

package com.github.robozonky.cli;

import java.util.Optional;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.confirmations.ConfirmationProviderService;
import com.github.robozonky.api.confirmations.RequestId;

import static org.mockito.Mockito.*;

public class TestingZonkoidProviderService implements ConfirmationProviderService {

    static final ConfirmationProvider INSTANCE = mock(ConfirmationProvider.class);

    @Override
    public Optional<ConfirmationProvider> find(final String providerId) {
        if (providerId.equals("zonkoid")) {
            return Optional.of(INSTANCE);
        } else {
            return Optional.empty();
        }
    }

    private static final class TestingZonkoidProvider implements ConfirmationProvider  {

        @Override
        public boolean requestConfirmation(final RequestId auth, final int loanId, final int amount) {
            return true;
        }

        @Override
        public String getId() {
            return "zonkoid";
        }
    }

}
