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
import java.util.Objects;
import java.util.stream.Stream;

public enum Rating implements BaseEnum {

    // it is imperative for proper functioning of strategy algorithms that ratings here be ordered best to worst
    AAAAA("A**", new BigDecimal("0.025")),
     AAAA("A*",  new BigDecimal("0.034")),
      AAA("A++", new BigDecimal("0.042")),
       AA("A+",  new BigDecimal("0.058")),
        A("A",   new BigDecimal("0.074")),
        B("B",   new BigDecimal("0.089")),
        C("C",   new BigDecimal("0.099")),
        D("D",   new BigDecimal("0.119"));

    public static Rating findByCode(final String code) {
        return Stream.of(Rating.values())
                .filter(r -> Objects.equals(r.code, code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown rating: " + code));
    }

    private final String code;
    private final BigDecimal expectedYield;

    // TODO somehow reflect change on expected yield post-August 2017
    Rating(final String code, final BigDecimal expectedYield) {
        this.code = code;
        this.expectedYield = expectedYield;
    }

    @Override
    public String getCode() {
        return code;
    }

    public BigDecimal getExpectedYield() {
        return expectedYield;
    }

}
