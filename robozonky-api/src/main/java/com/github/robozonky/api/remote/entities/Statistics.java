/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.api.remote.entities;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.github.robozonky.api.Ratio;

public interface Statistics {

    /**
     *
     * @return Empty if the user is relatively new on Zonky and they don't calculate this for them yet.
     */
    Optional<Ratio> getProfitability();

    List<RiskPortfolio> getRiskPortfolio();

    /**
     * Zonky only recalculates the information within this class every N hours.
     *
     * @return This method returns the timestamp of the last recalculation.
     */
    OffsetDateTime getTimestamp();
}
