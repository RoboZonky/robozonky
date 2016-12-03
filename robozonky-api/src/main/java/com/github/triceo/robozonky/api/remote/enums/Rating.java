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

package com.github.triceo.robozonky.api.remote.enums;

import java.math.BigDecimal;

public enum Rating {

    AAAAA("A**", new BigDecimal("0.025")),
     AAAA("A*",  new BigDecimal("0.034")),
      AAA("A++", new BigDecimal("0.042")),
       AA("A+",  new BigDecimal("0.058")),
        A("A",   new BigDecimal("0.074")),
        B("B",   new BigDecimal("0.089")),
        C("C",   new BigDecimal("0.099")),
        D("D",   new BigDecimal("0.119"));


    private final String code;
    private final BigDecimal expectedYield;

    Rating(final String code, final BigDecimal expectedYield) {
        this.code = code;
        this.expectedYield = expectedYield;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getExpectedYield() {
        return expectedYield;
    }

}
