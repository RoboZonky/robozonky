/*
 * Copyright 2019 The RoboZonky Project
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

import javax.xml.bind.annotation.XmlElement;
import java.util.Optional;

public class CurrentOverview extends BaseOverview {

    private long principalLeft;
    private long principalLeftToPay;
    private long principalLeftDue;
    private long interestPlanned;
    private long interestLeft;
    private long interestLeftToPay;
    private long interestLeftDue;
    @XmlElement
    private CurrentPortfolio currentInvestments;

    CurrentOverview() {
        // for JAXB
    }

    @XmlElement
    public long getPrincipalLeft() {
        return principalLeft;
    }

    @XmlElement
    public long getPrincipalLeftToPay() {
        return principalLeftToPay;
    }

    @XmlElement
    public long getPrincipalLeftDue() {
        return principalLeftDue;
    }

    @XmlElement
    public long getInterestPlanned() {
        return interestPlanned;
    }

    @XmlElement
    public long getInterestLeft() {
        return interestLeft;
    }

    @XmlElement
    public long getInterestLeftToPay() {
        return interestLeftToPay;
    }

    @XmlElement
    public long getInterestLeftDue() {
        return interestLeftDue;
    }

    public Optional<CurrentPortfolio> getCurrentInvestments() {
        return Optional.ofNullable(currentInvestments);
    }

}
