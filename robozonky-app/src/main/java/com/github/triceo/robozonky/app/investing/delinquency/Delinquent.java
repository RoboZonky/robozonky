/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.investing.delinquency;

import java.time.OffsetDateTime;
import java.util.Objects;

final class Delinquent {

    private final int loanId;
    private final OffsetDateTime since;

    public Delinquent(final int loanId, final OffsetDateTime since) {
        this.loanId = loanId;
        this.since = since;
    }

    public int getLoanId() {
        return loanId;
    }

    public OffsetDateTime getSince() {
        return since;
    }

    @Override
    public String toString() {
        return "Delinquent{" +
                "loanId=" + loanId +
                ", since=" + since +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Delinquent that = (Delinquent) o;
        return loanId == that.loanId &&
                Objects.equals(since, that.since);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId, since);
    }
}
