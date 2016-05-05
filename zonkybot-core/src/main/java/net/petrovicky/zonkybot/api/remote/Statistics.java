package net.petrovicky.zonkybot.api.remote;

import java.math.BigDecimal;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

public class Statistics {

    private BigDecimal currentProfitability, expectedProfitability;
    private Object currentOverview, overallOverview, overallPortfolio, cashFlow;
    private List<RiskPortfolio> riskPortfolio;

    @XmlElement
    public BigDecimal getCurrentProfitability() {
        return currentProfitability;
    }

    @XmlElement
    public BigDecimal getExpectedProfitability() {
        return expectedProfitability;
    }

    @XmlTransient
    public Object getCurrentOverview() {
        return currentOverview;
    }

    @XmlTransient
    public Object getOverallOverview() {
        return overallOverview;
    }

    @XmlTransient
    public Object getOverallPortfolio() {
        return overallPortfolio;
    }

    @XmlTransient
    public Object getCashFlow() {
        return cashFlow;
    }

    @XmlElementWrapper
    public List<RiskPortfolio> getRiskPortfolio() {
        return riskPortfolio;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Statistics{");
        sb.append("currentProfitability=").append(currentProfitability);
        sb.append(", expectedProfitability=").append(expectedProfitability);
        sb.append(", riskPortfolio=").append(riskPortfolio);
        sb.append('}');
        return sb.toString();
    }
}
