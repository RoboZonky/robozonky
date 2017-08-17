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

package com.github.triceo.robozonky.api.remote.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Statistics extends BaseEntity {

    private static BigDecimal getOrDefault(final BigDecimal actualValue) {
        return Statistics.getOrDefault(actualValue, () -> BigDecimal.ZERO);
    }

    private static <T> List<T> getOrDefault(final List<T> actualValue) {
        return actualValue == null ? Collections.emptyList() : actualValue;
    }

    private static <T> T getOrDefault(final T actualValue, final Supplier<T> defaultValue) {
        return actualValue == null ? defaultValue.get() : actualValue;
    }

    private BigDecimal currentProfitability, expectedProfitability;
    private CurrentOverview currentOverview;
    private OverallOverview overallOverview;
    private OverallPortfolio overallPortfolio;
    private List<Instalment> cashFlow;
    private List<RiskPortfolio> riskPortfolio;
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @XmlElement
    public BigDecimal getCurrentProfitability() {
        return Statistics.getOrDefault(this.currentProfitability);
    }

    @XmlElement
    public BigDecimal getExpectedProfitability() {
        return Statistics.getOrDefault(this.expectedProfitability);
    }

    @XmlElement
    public CurrentOverview getCurrentOverview() {
        return Statistics.getOrDefault(this.currentOverview, CurrentOverview::new);
    }

    @XmlElement
    public OverallOverview getOverallOverview() {
        return Statistics.getOrDefault(this.overallOverview, OverallOverview::new);
    }

    @XmlElement
    public OverallPortfolio getOverallPortfolio() {
        return Statistics.getOrDefault(this.overallPortfolio, OverallPortfolio::new);
    }

    @XmlElement
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * @return Expected cashflows for 8 previous months, the current month, and three future months. Current month is
     * on index 8, the next month after that is on index 9.
     */
    @XmlElementWrapper
    public List<Instalment> getCashFlow() {
        return Statistics.getOrDefault(this.cashFlow);
    }

    @XmlElementWrapper
    public Collection<RiskPortfolio> getRiskPortfolio() {
        return Statistics.getOrDefault(this.riskPortfolio);
    }
}
