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

package com.github.triceo.robozonky.remote;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RatingsTest {

    @Test
    public void ratingsAreProperlySerialized() {
        Assertions.assertThat(Ratings.of(Rating.A).toString()).isEqualTo("[\"A\"]");
        Assertions.assertThat(Ratings.of(Rating.AA, Rating.AAAA, Rating.AA).toString()).isEqualTo("[\"AAAA\", \"AA\"]");
    }

    @Test
    public void ratingsAreProperlyEnumerated() {
        Assertions.assertThat(Ratings.of(Rating.AAA).getRatings()).containsExactly(Rating.AAA);
        Assertions.assertThat(Ratings.of(Rating.B, Rating.C, Rating.B).getRatings()).containsExactly(Rating.B, Rating.C);
    }

}
