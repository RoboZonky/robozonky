/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky.remote;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class Statistics implements BaseEntity {

    private BigDecimal currentProfitability, expectedProfitability;
    private CurrentOverview currentOverview;
    private OverallOverview overallOverview;
    private OverallPortfolio overallPortfolio;
    private Collection<Instalment> cashFlow;
    private List<RiskPortfolio> riskPortfolio;

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

    @XmlElementWrapper
    public Collection<Instalment> getCashFlow() {
        return cashFlow;
    }

    @XmlElementWrapper
    public Collection<RiskPortfolio> getRiskPortfolio() {
        return riskPortfolio;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Statistics{");
        sb.append("currentProfitability=").append(currentProfitability);
        sb.append(", expectedProfitability=").append(expectedProfitability);
        sb.append(", currentOverview=").append(currentOverview);
        sb.append(", overallOverview=").append(overallOverview);
        sb.append(", overallPortfolio=").append(overallPortfolio);
        sb.append(", riskPortfolio=").append(riskPortfolio);
        sb.append(", cashFlow=").append(cashFlow);
        sb.append('}');
        return sb.toString();
    }
}
