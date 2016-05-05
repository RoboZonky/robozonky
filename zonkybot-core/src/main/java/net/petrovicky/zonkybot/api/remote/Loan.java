package net.petrovicky.zonkybot.api.remote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.map.annotate.JsonDeserialize;

public class Loan {

    @XmlElement
    public int getId() {
        return id;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    @XmlElement
    public String getStory() {
        return story;
    }

    @XmlElement
    public String getNickName() {
        return nickName;
    }

    @XmlElement
    public int getTermInMonths() {
        return termInMonths;
    }

    @XmlElement
    public BigDecimal getInterestRate() {
        return interestRate;
    }

    @XmlElement
    @JsonDeserialize(using = RatingDeserializer.class)
    public Rating getRating() {
        return rating;
    }

    @XmlElement
    public boolean isTopped() {
        return topped;
    }

    @XmlElement
    public double getAmount() {
        return amount;
    }

    @XmlElement
    public double getRemainingInvestment() {
        return remainingInvestment;
    }

    @XmlElement
    public boolean isCovered() {
        return covered;
    }

    @XmlElement
    public boolean isPublished() {
        return published;
    }

    @XmlElement
    @JsonDeserialize(using = InstantDeserializer.class)
    public Instant getDatePublished() {
        return datePublished;
    }

    @XmlElement
    @JsonDeserialize(using = InstantDeserializer.class)
    public Instant getDeadline() {
        return deadline;
    }

    @XmlElement
    public int getInvestmentsCount() {
        return investmentsCount;
    }

    @XmlElement
    public int getQuestionsCount() {
        return questionsCount;
    }

    @XmlElement
    public Collection<Photo> getPhotos() {
        return photos;
    }

    private int id;
    private String name;
    private String story;
    private String nickName;
    private int termInMonths;
    private BigDecimal interestRate;
    // FIXME will need converter
    private Rating rating;
    private boolean topped;
    private double amount;
    private double remainingInvestment;
    private boolean covered;
    private boolean published;
    private Instant datePublished;
    private Instant deadline;
    private int investmentsCount;
    private int questionsCount;
    private Collection<Photo> photos;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Loan{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", story='").append(story).append('\'');
        sb.append(", nickName='").append(nickName).append('\'');
        sb.append(", termInMonths=").append(termInMonths);
        sb.append(", interestRate=").append(interestRate);
        sb.append(", rating=").append(rating);
        sb.append(", topped=").append(topped);
        sb.append(", amount=").append(amount);
        sb.append(", remainingInvestment=").append(remainingInvestment);
        sb.append(", covered=").append(covered);
        sb.append(", published=").append(published);
        sb.append(", datePublished=").append(datePublished);
        sb.append(", deadline=").append(deadline);
        sb.append(", investmentsCount=").append(investmentsCount);
        sb.append(", questionsCount=").append(questionsCount);
        sb.append(", photos=").append(photos);
        sb.append('}');
        return sb.toString();
    }
}
