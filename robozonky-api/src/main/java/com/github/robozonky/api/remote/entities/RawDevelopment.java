/*
 * Copyright 2018 The RoboZonky Project
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

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.remote.enums.DevelopmentTpe;

/**
 * Represents a notification of collections department operating on delinquent loans. Obvious name for this class would
 * be "Collections" and Java developers will hopefully understand that this was rejected for reasons just as obvious.
 * Another obvious name would be InvestorEvent, which is how Zonky calls this in their API. Unfortunately, this would
 * clash with {@link Event} too much.
 */
public class RawDevelopment extends BaseEntity {

    private DevelopmentTpe businessCode;
    private String publicNote;
    private Object metadata;
    private DateDescriptor dateFrom, dateTo;

    @XmlElement
    public DevelopmentTpe getBusinessCode() {
        return businessCode;
    }

    @XmlElement
    public String getPublicNote() {
        return publicNote;
    }

    // TODO figure out details and implement
    @XmlElement
    public Object getMetadata() {
        return metadata;
    }

    @XmlElement
    public DateDescriptor getDateFrom() {
        return dateFrom;
    }

    @XmlElement
    public DateDescriptor getDateTo() {
        return dateTo;
    }
}
