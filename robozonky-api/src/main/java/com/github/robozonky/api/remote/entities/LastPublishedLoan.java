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

import java.time.OffsetDateTime;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.internal.util.DateUtil;

public class LastPublishedLoan extends BaseEntity {

    private int id;
    private OffsetDateTime datePublished;

    LastPublishedLoan() {
        // for JAXB
    }

    public LastPublishedLoan(final int loanId) {
        this(loanId, DateUtil.offsetNow());
    }

    public LastPublishedLoan(final int loanId, final OffsetDateTime datePublished) {
        this.id = loanId;
        this.datePublished = datePublished;
    }

    @XmlElement
    public int getId() {
        return id;
    }

    @XmlElement
    public OffsetDateTime getDatePublished() {
        return datePublished;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final LastPublishedLoan that = (LastPublishedLoan) o;
        return id == that.id &&
                Objects.equals(datePublished, that.datePublished);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, datePublished);
    }
}
