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
package com.github.triceo.robozonky.remote;

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CurrentOverview{");
        sb.append("principalLeft=").append(principalLeft);
        sb.append(", principalLeftToPay=").append(principalLeftToPay);
        sb.append(", principalLeftDue=").append(principalLeftDue);
        sb.append(", interestPlanned=").append(interestPlanned);
        sb.append(", interestLeft=").append(interestLeft);
        sb.append(", interestLeftToPay=").append(interestLeftToPay);
        sb.append(", interestLeftDue=").append(interestLeftDue);
        sb.append("} extends ");
        sb.append(super.toString());
        return sb.toString();
    }
}
