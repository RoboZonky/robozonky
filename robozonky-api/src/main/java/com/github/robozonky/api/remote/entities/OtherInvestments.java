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

import com.github.robozonky.api.Money;
import io.vavr.Lazy;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Collection;
import java.util.Collections;

public class OtherInvestments extends BaseEntity {

    @XmlElement
    private String amount;
    private final Lazy<Money> moneyAmount = Lazy.of(() -> Money.from(amount));
    private Collection<String> otherBorrowerNicknames = Collections.emptyList();

    @XmlElement
    public Collection<String> getOtherBorrowerNicknames() {
        return otherBorrowerNicknames;
    }

    @XmlTransient
    public Money getAmount() {
        return moneyAmount.get();
    }
}
