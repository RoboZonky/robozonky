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

package com.github.robozonky.internal.remote.entities;

import com.github.robozonky.api.remote.entities.Development;
import com.github.robozonky.api.remote.enums.DevelopmentType;

import java.time.OffsetDateTime;
import java.util.Optional;

public final class MutableDevelopment extends Development {

    private OffsetDateTime dateFrom;
    private OffsetDateTime dateTo;

    public void setBusinessCode(final DevelopmentType businessCode) {
        this.businessCode = businessCode;
    }

    public void setPublicNote(final String publicNote) {
        this.publicNote = publicNote;
    }

    public void setMetadata(final Object metadata) {
        this.metadata = metadata;
    }

    public void setLoanId(final int loanId) {
        this.loanId = loanId;
    }

    public void setDateFrom(final OffsetDateTime dateFrom) {
        this.dateFrom = dateFrom;
    }

    public void setDateTo(final OffsetDateTime dateTo) {
        this.dateTo = dateTo;
    }

    @Override
    public OffsetDateTime getDateFrom() {
        return dateFrom;
    }

    @Override
    public Optional<OffsetDateTime> getDateTo() {
        return Optional.ofNullable(dateTo);
    }
}
