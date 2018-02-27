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

import java.math.BigDecimal;
import java.util.Optional;

import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.util.Refreshable;

class RefreshableBalance extends Refreshable<BigDecimal> {

    private final Authenticated auth;

    public RefreshableBalance(final Authenticated authenticated) {
        this.auth = authenticated;
    }

    @Override
    protected String getLatestSource() {
        return auth.call(zonky -> zonky.getWallet().getAvailableBalance()).toString();
    }

    @Override
    protected Optional<BigDecimal> transform(final String source) {
        return Optional.of(new BigDecimal(source));
    }
}
