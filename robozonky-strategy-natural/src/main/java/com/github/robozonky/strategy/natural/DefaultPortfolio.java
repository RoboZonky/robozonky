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

package com.github.robozonky.strategy.natural;

import java.util.HashMap;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;

enum DefaultPortfolio {

    CONSERVATIVE(3, 13, 19, 21, 19, 11, 7, 5, 1.5, 0.5, 0),
    BALANCED(2, 6, 14, 16, 18, 15, 12, 9, 5, 2, 1),
    PROGRESSIVE(1, 2, 7, 10, 14, 15, 17, 15, 10, 6, 3),
    EMPTY(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    private final HashMap<Ratio, Ratio> shares = new HashMap<>();

    DefaultPortfolio(double aaaaaa, final double aaaaa, final double aaaa, final double aaa, final double aae,
            final double aa, final double ae, final double a, final double b, final double c, final double d) {
        shares.put(Rating.AAAAAA.getInterestRate(), Ratio.fromPercentage(aaaaaa));
        shares.put(Rating.AAAAA.getInterestRate(), Ratio.fromPercentage(aaaaa));
        shares.put(Rating.AAAA.getInterestRate(), Ratio.fromPercentage(aaaa));
        shares.put(Rating.AAA.getInterestRate(), Ratio.fromPercentage(aaa));
        shares.put(Rating.AAE.getInterestRate(), Ratio.fromPercentage(aae));
        shares.put(Rating.AA.getInterestRate(), Ratio.fromPercentage(aa));
        shares.put(Rating.AE.getInterestRate(), Ratio.fromPercentage(ae));
        shares.put(Rating.A.getInterestRate(), Ratio.fromPercentage(a));
        shares.put(Rating.B.getInterestRate(), Ratio.fromPercentage(b));
        shares.put(Rating.C.getInterestRate(), Ratio.fromPercentage(c));
        shares.put(Rating.D.getInterestRate(), Ratio.fromPercentage(d));
    }

    public Ratio getDefaultShare(final Ratio r) {
        if (shares.containsKey(r)) {
            return shares.get(r);
        } else {
            throw new IllegalStateException("Interest rate " + r + " is missing. This is a bug in RoboZonky.");
        }
    }

}
