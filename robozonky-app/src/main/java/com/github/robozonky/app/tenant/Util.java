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

package com.github.robozonky.app.tenant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collector;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.util.BigDecimalCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;

final class Util {

    private static final Logger LOGGER = LogManager.getLogger(Util.class);
    private static final Collector<BigDecimal, ?, BigDecimal> BIGDECIMAL_REDUCING_COLLECTOR =
            reducing(BigDecimal.ZERO, BigDecimalCalculator::plus);

    private Util() {
        // no instances
    }

    static Map<Rating, BigDecimal> getAmountsBlocked(final Tenant tenant) {
        final Select select = new Select()
                .lessThanOrNull("activeFrom", Instant.EPOCH.atZone(Defaults.ZONE_ID).toOffsetDateTime());
        return tenant.call(zonky -> zonky.getInvestments(select))
                .peek(investment -> LOGGER.debug("Found: {}.", investment))
                .collect(groupingBy(Investment::getRating, () -> new EnumMap<>(Rating.class),
                        mapping(Investment::getOriginalPrincipal, BIGDECIMAL_REDUCING_COLLECTOR)));
    }
}
