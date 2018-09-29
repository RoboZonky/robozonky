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

package com.github.robozonky.strategy.natural;

import java.util.EnumMap;

import com.github.robozonky.api.remote.enums.Rating;

enum DefaultPortfolio {

    CONSERVATIVE(3, 6, 16, 25, 20, 15, 15, 0),
    BALANCED(1, 3, 17, 20, 25, 20, 12, 2),
    PROGRESSIVE(0, 2, 13, 15, 20, 25, 20, 5),
    EMPTY(0, 0, 0, 0, 0, 0, 0, 0);

    private final EnumMap<Rating, Integer> shares = new EnumMap<>(Rating.class);

    DefaultPortfolio(final int aaaaa, final int aaaa, final int aaa, final int aa, final int a, final int b,
                     final int c,
                     final int d) {
        shares.put(Rating.AAAAA, aaaaa);
        shares.put(Rating.AAAA, aaaa);
        shares.put(Rating.AAA, aaa);
        shares.put(Rating.AA, aa);
        shares.put(Rating.A, a);
        shares.put(Rating.B, b);
        shares.put(Rating.C, c);
        shares.put(Rating.D, d);
    }

    public int getDefaultShare(final Rating r) {
        if (shares.containsKey(r)) {
            return shares.get(r);
        } else {
            throw new IllegalStateException("Rating " + r + " is missing. This is a bug in RoboZonky.");
        }
    }

}
