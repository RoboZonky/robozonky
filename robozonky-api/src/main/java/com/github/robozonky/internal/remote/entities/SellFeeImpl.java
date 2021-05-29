/*
 * Copyright 2021 The RoboZonky Project
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

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbProperty;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.SellFee;
import com.github.robozonky.internal.test.DateUtil;

public class SellFeeImpl implements SellFee {

    @JsonbProperty(nillable = true)
    private OffsetDateTime expiresAt;

    private Money value;

    public SellFeeImpl() {
        // For JSON-B.
    }

    public SellFeeImpl(Money fee) {
        this.value = Objects.requireNonNull(fee);
    }

    @Override
    public Optional<OffsetDateTime> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    @Override
    public Money getValue() {
        return value;
    }

    public void setExpiresAt(final OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setValue(final Money value) {
        this.value = value;
    }

    @Override
    public String toString() {
        var expiryString = getExpiresAt()
            .map(DateUtil::toString)
            .map(s -> '\'' + s + '\'')
            .orElse("N/A");
        return new StringJoiner(", ", SellFeeImpl.class.getSimpleName() + "[", "]")
            .add("value='" + value + "'")
            .add("expiresAt=" + expiryString)
            .toString();
    }
}
