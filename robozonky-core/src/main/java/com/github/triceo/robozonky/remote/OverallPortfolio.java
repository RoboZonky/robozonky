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

public class OverallPortfolio implements BaseEntity {

    private int unpaid, paid, due;

    OverallPortfolio() {
        // for JAXB
    }

    public OverallPortfolio(int paid, int unpaid, int due) {
        this.unpaid = unpaid;
        this.paid = paid;
        this.due = due;
    }

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OverallPortfolio{");
        sb.append("unpaid=").append(unpaid);
        sb.append(", paid=").append(paid);
        sb.append(", due=").append(due);
        sb.append('}');
        return sb.toString();
    }
}
