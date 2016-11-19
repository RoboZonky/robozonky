/*
 * Copyright 2016 Lukáš Petrovický
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

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class carries several enumeration-based fields. Some of the enums are extremely important to the core function
 * (such as {@link Rating}), while others ({@link Region}, {@link MainIncomeType}, {@link Purpose}) are only providing
 * additional metadata. If the important enums change, we need RoboZonky to fail. However, in case of the others, we
 * provide non-failing deserializers which handle the missing values gracefully and provide a message warning users that
 * something needs an upgrade.
 */
public class Loan implements BaseEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(Loan.class);

    private static class RegionDeserializer extends JsonDeserializer<Region> {

        @Override
        public Region deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final String id = jsonParser.getText();
            try {
                final int actualId = Integer.parseInt(id) - 1; // regions in Zonky API are indexed from 1
                return Region.values()[actualId];
            } catch (final RuntimeException ex) { // whatever went wrong, don't fail on this unimportant enum
                Loan.LOGGER.warn("Unknown value '{}' for {}, API may be incomplete.", id, Region.class);
                return Region.UNKNOWN;
            }
        }

    }

    private static class PurposeDeserializer extends JsonDeserializer<Purpose> {

        @Override
        public Purpose deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final String id = jsonParser.getText();
            try {
                final int actualId = Integer.parseInt(id) - 1; // purposes in Zonky API are indexed from 1
                return Purpose.values()[actualId];
            } catch (final RuntimeException ex) { // whatever went wrong, don't fail on this unimportant enum
                Loan.LOGGER.warn("Unknown value '{}' for {}, API may be incomplete.", id, Purpose.class);
                return Purpose.JINE;
            }
        }

    }

    private static class MainIncomeTypeDeserializer extends JsonDeserializer<MainIncomeType> {

        @Override
        public MainIncomeType deserialize(final JsonParser jsonParser,
                                          final DeserializationContext deserializationContext)
                throws IOException {
            final String id = jsonParser.getText();
            try {
                return MainIncomeType.valueOf(id);
            } catch (final IllegalArgumentException ex) { // whatever went wrong, don't fail on this unimportant enum
                Loan.LOGGER.warn("Unknown value '{}' for {}, API may be incomplete.", id, MainIncomeType.class);
                return MainIncomeType.OTHERS_MAIN;
            }
        }

    }

    private boolean topped, covered, published;
    private int id, termInMonths, investmentsCount, questionsCount, userId;
    private double amount, remainingInvestment;
    private String name, story, nickName;
    private BigDecimal interestRate;
    private OffsetDateTime datePublished, deadline;
    private Rating rating;
    private Collection<Photo> photos;
    private BigDecimal investmentRate;
    private MyInvestment myInvestment;
    private MainIncomeType mainIncomeType;
    private Region region;
    private Purpose purpose;

    @XmlElement
    public MyInvestment getMyInvestment() {
        return myInvestment;
    }

    @XmlElement
    @JsonDeserialize(using = Loan.MainIncomeTypeDeserializer.class)
    public MainIncomeType getMainIncomeType() {
        return mainIncomeType;
    }

    @XmlElement
    public BigDecimal getInvestmentRate() {
        return investmentRate;
    }

    @XmlElement
    @JsonDeserialize(using = Loan.RegionDeserializer.class)
    public Region getRegion() {
        return region;
    }

    @XmlElement
    @JsonDeserialize(using = Loan.PurposeDeserializer.class)
    public Purpose getPurpose() {
        return purpose;
    }

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
    public OffsetDateTime getDatePublished() {
        return datePublished;
    }

    @XmlElement
    public OffsetDateTime getDeadline() {
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

    @XmlElement
    public int getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Loan{");
        sb.append("id=").append(id);
        sb.append(", termInMonths=").append(termInMonths);
        sb.append(", userId=").append(userId);
        sb.append(", amount=").append(amount);
        sb.append(", rating=").append(rating);
        sb.append('}');
        return sb.toString();
    }

}
