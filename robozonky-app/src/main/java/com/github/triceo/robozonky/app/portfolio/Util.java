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

package com.github.triceo.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.enums.TransactionCategory;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.internal.api.Retriever;

class Util {

    public static LocalDate getYesterdayIfAfter(final Duration timeFromMidnightToday) {
        final LocalDate now = LocalDate.now();
        final ZonedDateTime targetTimeToday = now.atStartOfDay(Defaults.ZONE_ID).plus(timeFromMidnightToday);
        return Instant.now().isAfter(targetTimeToday.toInstant()) ? now : now.minusDays(1);
    }

    /**
     * Blocked amounts represent loans in various stages. Either the user has invested and the loan has not yet been
     * funded to 100 % ("na tržišti"), or the user invested and the loan has been funded ("na cestě"). In the latter
     * case, the loan has already disappeared from the marketplace, which means that it will not be available for
     * investing any more. As far as we know, the next stage is "v pořádku", the blocked amount is cleared and the loan
     * becomes an active investment.
     * <p>
     * Based on that, this method deals with the first case - when the loan is still available for investing, but we've
     * already invested as evidenced by the blocked amount. It also unnecessarily deals with the second case, since
     * that is represented by a blocked amount as well. But that does no harm.
     * <p>
     * In case user has made repeated investments into a particular loan, this will show up as multiple blocked amounts.
     * The method needs to handle this as well.
     * @param api Authenticated Zonky API to read data from.
     * @return Every blocked amount represents a future investment. This method returns such investments.
     */
    public static List<Investment> retrieveInvestmentsRepresentedByBlockedAmounts(final Zonky api) {
        // first group all blocked amounts by the loan ID and sum them
        final Map<Integer, BigDecimal> amountsBlockedByLoans =
                api.getBlockedAmounts()
                        .filter(blocked -> blocked.getCategory() == TransactionCategory.INVESTMENT)
                        .collect(Collectors.groupingBy(BlockedAmount::getLoanId,
                                                       Collectors.reducing(BigDecimal.ZERO, BlockedAmount::getAmount,
                                                                           BigDecimal::add)));
        // and then fetch all the loans in parallel, converting them into investments
        return amountsBlockedByLoans.entrySet().parallelStream()
                .map(entry ->
                             Retriever.retrieve(() -> Optional.of(api.getLoan(entry.getKey())))
                                     .map(l -> new Investment(l, entry.getValue().intValue()))
                                     .orElseThrow(() -> new RuntimeException("Loan retrieval failed."))
                ).collect(Collectors.toList());
    }

}
