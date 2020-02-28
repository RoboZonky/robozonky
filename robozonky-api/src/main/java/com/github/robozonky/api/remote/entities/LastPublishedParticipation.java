/*
 * Copyright 2020 The RoboZonky Project
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

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.internal.test.DateUtil;

public class LastPublishedParticipation extends BaseEntity {

    private int id;
    // Expensive to deserialize, do it on-demand.
    @XmlElement
    private String datePublished;

    LastPublishedParticipation() {
        // for JAXB
    }

    public LastPublishedParticipation(final int loanId) {
        this(loanId, DateUtil.offsetNow());
    }

    public LastPublishedParticipation(final int loanId, final OffsetDateTime datePublished) {
        this.id = loanId;
        this.datePublished = datePublished.toString();
    }

    @XmlElement
    public int getId() {
        return id;
    }

    @XmlElement
    public OffsetDateTime getDatePublished() {
        return OffsetDateTimeAdapter.fromString(datePublished);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final LastPublishedParticipation that = (LastPublishedParticipation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LastPublishedParticipation.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("datePublished=" + datePublished)
                .toString();
    }
}
