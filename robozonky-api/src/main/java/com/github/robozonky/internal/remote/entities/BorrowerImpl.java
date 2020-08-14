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

package com.github.robozonky.internal.remote.entities;

import static java.util.Objects.requireNonNull;

import java.util.StringJoiner;

import com.github.robozonky.api.remote.entities.Borrower;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Region;

public class BorrowerImpl implements Borrower {

    private MainIncomeType primaryIncomeType;
    private Region region;

    public BorrowerImpl() {
        // For JSON-B.
    }

    public BorrowerImpl(final MainIncomeType mainIncomeType, final Region region) {
        this.primaryIncomeType = mainIncomeType;
        this.region = region;
    }

    @Override
    public MainIncomeType getPrimaryIncomeType() {
        return requireNonNull(primaryIncomeType);
    }

    public void setPrimaryIncomeType(final MainIncomeType primaryIncomeType) {
        this.primaryIncomeType = primaryIncomeType;
    }

    @Override
    public Region getRegion() {
        return requireNonNull(region);
    }

    public void setRegion(final Region region) {
        this.region = region;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BorrowerImpl.class.getSimpleName() + "[", "]")
            .add("primaryIncomeType=" + primaryIncomeType)
            .add("region=" + region)
            .toString();
    }
}
