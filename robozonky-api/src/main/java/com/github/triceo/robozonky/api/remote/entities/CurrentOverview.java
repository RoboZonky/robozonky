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

import javax.xml.bind.annotation.XmlElement;

public class CurrentOverview extends BaseOverview {

    private int principalLeft, principalLeftToPay, principalLeftDue, interestPlanned, interestLeft, interestLeftToPay,
            interestLeftDue;

    CurrentOverview() {
        // for JAXB
    }

    @XmlElement
    public int getPrincipalLeft() {
        return principalLeft;
    }

    @XmlElement
    public int getPrincipalLeftToPay() {
        return principalLeftToPay;
    }

    @XmlElement
    public int getPrincipalLeftDue() {
        return principalLeftDue;
    }

    @XmlElement
    public int getInterestPlanned() {
        return interestPlanned;
    }

    @XmlElement
    public int getInterestLeft() {
        return interestLeft;
    }

    @XmlElement
    public int getInterestLeftToPay() {
        return interestLeftToPay;
    }

    @XmlElement
    public int getInterestLeftDue() {
        return interestLeftDue;
    }
}
