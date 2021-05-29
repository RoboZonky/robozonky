/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.internal.remote.entities;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;

import javax.json.bind.annotation.JsonbProperty;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.RiskPortfolio;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.util.functional.Memoizer;

public class StatisticsImpl implements Statistics {

    private static final Supplier<Statistics> EMPTY = Memoizer.memoize(StatisticsImpl::emptyAndFresh);

    @JsonbProperty(nillable = true)
    private Ratio profitability;
    private List<RiskPortfolioImpl> riskPortfolio;
    private OffsetDateTime timestamp;

    public StatisticsImpl() {
        // For JSON-B.
    }

    public static Statistics empty() {
        return EMPTY.get();
    }

    public static Statistics emptyAndFresh() {
        final StatisticsImpl s = new StatisticsImpl();
        s.profitability = Ratio.ZERO;
        s.riskPortfolio = Collections.emptyList();
        s.timestamp = DateUtil.zonedNow()
            .toOffsetDateTime();
        return s;
    }

    @Override
    public Optional<Ratio> getProfitability() {
        return Optional.ofNullable(profitability);
    }

    public void setProfitability(final Ratio profitability) {
        this.profitability = profitability;
    }

    @Override
    public List<RiskPortfolio> getRiskPortfolio() { // "riskPortfolio" is null for new Zonky users
        return riskPortfolio == null ? Collections.emptyList() : Collections.unmodifiableList(riskPortfolio);
    }

    public void setRiskPortfolio(final List<RiskPortfolioImpl> riskPortfolio) {
        this.riskPortfolio = riskPortfolio;
    }

    @Override
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StatisticsImpl.class.getSimpleName() + "[", "]")
            .add("profitability=" + profitability)
            .add("riskPortfolio=" + riskPortfolio)
            .add("timestamp='" + DateUtil.toString(timestamp) + "'")
            .toString();
    }
}
