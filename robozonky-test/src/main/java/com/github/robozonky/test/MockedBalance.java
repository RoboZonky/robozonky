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

package com.github.robozonky.test;

import java.math.BigDecimal;

import com.github.robozonky.common.RemoteBalance;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MockedBalance implements RemoteBalance {

    private static Logger LOGGER = LoggerFactory.getLogger(MockedBalance.class);

    private final Zonky zonky;
    private BigDecimal difference = BigDecimal.ZERO;

    public MockedBalance(final Zonky zonky) {
        this.zonky = zonky;
    }

    @Override
    public void update(final BigDecimal change, final boolean isEphemeral) {
        difference = difference.add(change);
        LOGGER.debug("New difference is {}.", difference);
    }

    @Override
    public BigDecimal get() {
        final BigDecimal result = zonky.getWallet().getAvailableBalance().add(difference);
        LOGGER.debug("Retrieving balance of {}.", result);
        return result;
    }

 }
