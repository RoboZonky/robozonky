package net.petrovicky.zonkybot.api.remote;

import javax.xml.bind.annotation.XmlElement;

public class RiskPortfolio {

    private int unpaid, paid, due, totalAmount;
    private String rating;

    @XmlElement
    public int getUnpaid() {
        return unpaid;
    }

    @XmlElement
    public int getPaid() {
        return paid;
    }

    @XmlElement
    public int getDue() {
        return due;
    }

    @XmlElement
    public int getTotalAmount() {
        return totalAmount;
    }

    @XmlElement
    public String getRating() {
        return rating;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RiskPortfolio{");
        sb.append("unpaid=").append(unpaid);
        sb.append(", paid=").append(paid);
        sb.append(", due=").append(due);
        sb.append(", totalAmount=").append(totalAmount);
        sb.append(", rating='").append(rating).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
