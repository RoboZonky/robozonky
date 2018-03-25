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

import java.math.BigDecimal;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * The point of this class is to reuse known BigDecimal values (such as {@link BigDecimal#ZERO}) instead of creating
 * new instances for them. This will save significant amount of memory.
 * @see <a href="https://stackoverflow.com/questions/2501176/java-bigdecimal-memory-usage">BigDecimal memory
 * footprint</a>.
 */
class BigDecimalAdapter extends XmlAdapter<String, BigDecimal> {

    @Override
    public BigDecimal unmarshal(final String s) {
        final BigDecimal d = new BigDecimal(s);
        if (d.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        } else if (d.compareTo(BigDecimal.ONE) == 0) {
            return BigDecimal.ONE;
        } else if (d.compareTo(BigDecimal.TEN) == 0) {
            return BigDecimal.TEN;
        } else {
            return d;
        }
    }

    @Override
    public String marshal(final BigDecimal bigDecimal) {
        return bigDecimal.toPlainString();
    }
}
