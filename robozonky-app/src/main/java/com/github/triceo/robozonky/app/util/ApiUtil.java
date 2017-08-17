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

package com.github.triceo.robozonky.app.util;

import java.math.BigDecimal;

import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.internal.api.Settings;

public class ApiUtil {

    public static BigDecimal getLiveBalance(final Zonky api) {
        return api.getWallet().getAvailableBalance();
    }

    public static BigDecimal getDryRunBalance(final Zonky api) {
        final int balance = Settings.INSTANCE.getDefaultDryRunBalance();
        return (balance > -1) ? BigDecimal.valueOf(balance) : ApiUtil.getLiveBalance(api);
    }

}
