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

package com.github.robozonky.api.remote.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.internal.api.Defaults;

/**
 * This class carries several enumeration-based fields. Some of the enums are extremely important to the core function
 * (such as {@link Rating}), while others ({@link Region}, {@link MainIncomeType}, {@link Purpose}) are only providing
 * additional metadata. If the important enums change, we need RoboZonky to fail. However, in case of the others, we
 * provide non-failing deserializers which handle the missing values gracefully and provide a message warning users that
 * something needs an upgrade.
 */
public class Loan extends BaseEntity {

    private static final Function<Integer, String> LOAN_URL_SUPPLIER =
            (id) -> "https://app.zonky.cz/#/marketplace/detail/" + id + "/";

    /**
     * Zonky's API documentation states that {@link #getUrl()} is optional. Therefore the only safe use of that
     * attribute is through this method.
     * @return URL to a loan on Zonky's website. Guessed if not present.
     */
    public static String getUrlSafe(final Loan l) {
        // in case investment has no loan, we guess loan URL
        final String providedUrl = l.getUrl();
        return providedUrl == null ? Loan.LOAN_URL_SUPPLIER.apply(l.getId()) : providedUrl;
    }

    private boolean topped, covered, published, questionsAllowed;
    private int id, termInMonths = 1, investmentsCount, questionsCount, userId, activeLoansCount;
    private double amount, remainingInvestment;
    private String name, nickName, story, url;
    private BigDecimal interestRate = new BigDecimal("0.1999");
    private OffsetDateTime datePublished = OffsetDateTime.now(), deadline = datePublished.plusDays(2);
    private Rating rating = Rating.D;
    private Collection<Photo> photos = Collections.emptyList();
    private BigDecimal investmentRate = BigDecimal.ZERO;
    private MyInvestment myInvestment;
    private OtherInvestments myOtherInvestments;
    private MainIncomeType mainIncomeType = MainIncomeType.OTHERS_MAIN;
    private Region region = Region.UNKNOWN;
    private Purpose purpose = Purpose.JINE;

    protected Loan() {
        // for JAXB
    }

    public Loan(final int id, final int amount) { // creates a simple "fake" loan
        this(id, amount, OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
    }

    public Loan(final int id, final int amount, final OffsetDateTime datePublished) { // creates a simple "fake" loan
        this.id = id;
        this.amount = amount;
        this.remainingInvestment = amount;
        this.datePublished = datePublished;
    }

    @XmlElement
    public MyInvestment getMyInvestment() {
        return myInvestment;
    }

    @XmlElement
    public MainIncomeType getMainIncomeType() {
        return mainIncomeType;
    }

    @XmlElement
    public BigDecimal getInvestmentRate() {
        return investmentRate;
    }

    @XmlElement
    public Region getRegion() {
        return region;
    }

    @XmlElement
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
    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    @XmlElement
    public int getQuestionsCount() {
        return questionsCount;
    }

    @XmlElement
    public boolean isQuestionsAllowed() {
        return questionsAllowed;
    }

    @XmlElement
    public Collection<Photo> getPhotos() {
        return photos;
    }

    @XmlElement
    public OtherInvestments getMyOtherInvestments() {
        return myOtherInvestments;
    }

    @XmlElement
    public int getUserId() {
        return userId;
    }

    /**
     * Return loan URL provided by Zonky API. Also see {@link #getUrlSafe(Loan)}.
     * @return Zonky URL as provided by the API.
     */
    @XmlElement
    public String getUrl() {
        return url;
    }
}
