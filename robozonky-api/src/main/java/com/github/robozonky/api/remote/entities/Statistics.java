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

package com.github.robozonky.api.remote.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.internal.util.DateUtil;
import io.vavr.Lazy;

public class Statistics extends BaseEntity {

    private static final Lazy<Statistics> EMPTY = Lazy.of(() -> {
        final Statistics s = new Statistics();
        s.currentProfitability = BigDecimal.ZERO;
        s.expectedProfitability = BigDecimal.ZERO;
        s.cashFlow = Collections.emptyList();
        s.riskPortfolio = Collections.emptyList();
        s.expectedPayments = Collections.emptyList();
        s.currentOverview = new CurrentOverview();
        s.overallOverview = new OverallOverview();
        s.overallPortfolio = new OverallPortfolio(0, 0, 0);
        s.superInvestorOverview = SuperInvestorOverview.empty();
        s.timestamp = DateUtil.offsetNow();
        return s;
    });

    private BigDecimal currentProfitability, expectedProfitability;
    private CurrentOverview currentOverview;
    private OverallOverview overallOverview;
    private OverallPortfolio overallPortfolio;
    private SuperInvestorOverview superInvestorOverview;
    private List<RiskPortfolio> riskPortfolio;
    private List<Instalment> cashFlow;
    private List<ExpectedPayment> expectedPayments;
    private OffsetDateTime timestamp;

    private Statistics() {
        // for JAXB
    }

    public static Statistics empty() {
        return EMPTY.get();
    }

    private static <T> List<T> unmodifiableOrEmpty(final List<T> possiblyNull) {
        return possiblyNull == null ? Collections.emptyList() : Collections.unmodifiableList(possiblyNull);
    }

    @XmlElement
    public BigDecimal getCurrentProfitability() {
        return currentProfitability;
    }

    @XmlElement
    public BigDecimal getExpectedProfitability() {
        return expectedProfitability;
    }

    @XmlElement
    public CurrentOverview getCurrentOverview() {
        return currentOverview;
    }

    @XmlElement
    public OverallOverview getOverallOverview() {
        return overallOverview;
    }

    @XmlElement
    public OverallPortfolio getOverallPortfolio() {
        return overallPortfolio;
    }

    @XmlElement
    public List<RiskPortfolio> getRiskPortfolio() { // "riskPortfolio" is null for new Zonky users
        return unmodifiableOrEmpty(riskPortfolio);
    }

    @XmlElement
    public SuperInvestorOverview getSuperInvestorOverview() {
        return superInvestorOverview;
    }

    @XmlElement
    public List<Instalment> getCashFlow() {
        return unmodifiableOrEmpty(cashFlow);
    }

    @XmlElement
    public List<ExpectedPayment> getExpectedPayments() {
        return unmodifiableOrEmpty(expectedPayments);
    }

    @XmlElement
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
}
