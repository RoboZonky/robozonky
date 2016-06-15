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

    @Test
    public void ofAll() {
        final Ratings result = Ratings.all();
        Assertions.assertThat(result.getRatings()).containsOnly(Rating.values());
    }

    @Test
    public void correctValueOf() {
        // test no items
        Assertions.assertThat(Ratings.valueOf("[]").getRatings()).isEmpty();
        Assertions.assertThat(Ratings.valueOf(" [ ] ").getRatings()).isEmpty();
        // test one item
        Assertions.assertThat(Ratings.valueOf("[ \"A\" ]").getRatings())
                .containsExactly(Rating.A);
        Assertions.assertThat(Ratings.valueOf(" [ \"AA\"]").getRatings())
                .containsExactly(Rating.AA);
        // test multiple items
        Assertions.assertThat(Ratings.valueOf(" [\"AAA\", \"B\"]").getRatings())
                .containsExactly(Rating.AAA, Rating.B);
        Assertions.assertThat(Ratings.valueOf(" [\"B\" , \"AAA\"] ").getRatings())
                .containsExactly(Rating.AAA, Rating.B);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidValueOf() {
        Ratings.valueOf("[");
    }

    @Test(expected = IllegalArgumentException.class)
    public void unquotedValueOf() {
        Ratings.valueOf("[A]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownValueOf() {
        Ratings.valueOf("[\"SOME_UNKNOWN_VALUE\"]");
    }
}
