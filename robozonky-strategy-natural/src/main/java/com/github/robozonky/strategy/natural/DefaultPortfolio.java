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

package com.github.robozonky.strategy.natural;

import java.util.EnumMap;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;

enum DefaultPortfolio {

    CONSERVATIVE(16, 19, 21, 19, 11, 7, 5, 1.5, 0.5, 0),
    BALANCED(8, 14, 16, 18, 15, 12, 9, 5, 2, 1),
    PROGRESSIVE(3, 7, 10, 14, 15, 17, 15, 10, 6, 3),
    EMPTY(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    private final EnumMap<Rating, Ratio> shares = new EnumMap<>(Rating.class);

    DefaultPortfolio(final double aaaaa, final double aaaa, final double aaa, final double aae, final double aa,
                     final double ae, final double a, final double b, final double c, final double d) {
        shares.put(Rating.AAAAA, Ratio.fromPercentage(aaaaa));
        shares.put(Rating.AAAA, Ratio.fromPercentage(aaaa));
        shares.put(Rating.AAA, Ratio.fromPercentage(aaa));
        shares.put(Rating.AAE, Ratio.fromPercentage(aae));
        shares.put(Rating.AA, Ratio.fromPercentage(aa));
        shares.put(Rating.AE, Ratio.fromPercentage(ae));
        shares.put(Rating.A, Ratio.fromPercentage(a));
        shares.put(Rating.B, Ratio.fromPercentage(b));
        shares.put(Rating.C, Ratio.fromPercentage(c));
        shares.put(Rating.D, Ratio.fromPercentage(d));
    }

    public Ratio getDefaultShare(final Rating r) {
        if (shares.containsKey(r)) {
            return shares.get(r);
        } else {
            throw new IllegalStateException("Rating " + r + " is missing. This is a bug in RoboZonky.");
        }
    }

}
