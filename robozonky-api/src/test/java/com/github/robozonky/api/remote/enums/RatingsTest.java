/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.api.remote.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

class RatingsTest {

    @Test
    void ratingsAreProperlySerialized() {
        assertThat(Ratings.of(Rating.A).toString()).isEqualTo("[\"A\"]");
        assertThat(Ratings.of(Rating.AA, Rating.AAAA, Rating.AA).toString()).isEqualTo("[\"AAAA\", \"AA\"]");
    }

    @Test
    void ratingsAreProperlyEnumerated() {
        assertThat(Ratings.of(Rating.AAA).getRatings()).containsExactly(Rating.AAA);
        assertThat(Ratings.of(Rating.B, Rating.C, Rating.B).getRatings()).containsExactly(Rating.B,
                                                                                                     Rating.C);
    }

    @Test
    void ofAll() {
        final Ratings result = Ratings.all();
        assertThat(result.getRatings()).containsOnly(Rating.values());
    }

    @Test
    void correctValueOf() {
        // test no items
        assertThat(Ratings.valueOf("[]").getRatings()).isEmpty();
        assertThat(Ratings.valueOf(" [ ] ").getRatings()).isEmpty();
        // test one item
        assertThat(Ratings.valueOf("[ \"A\" ]").getRatings())
                .containsExactly(Rating.A);
        assertThat(Ratings.valueOf(" [ \"AA\"]").getRatings())
                .containsExactly(Rating.AA);
        // test multiple items
        assertThat(Ratings.valueOf(" [\"AAA\", \"B\"]").getRatings())
                .containsExactly(Rating.AAA, Rating.B);
        assertThat(Ratings.valueOf(" [\"B\" , \"AAA\"] ").getRatings())
                .containsExactly(Rating.AAA, Rating.B);
    }

    @Test
    void invalidValueOf() {
        assertThatThrownBy(() -> Ratings.valueOf("[")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidValueOf2() {
        assertThatThrownBy(() -> Ratings.valueOf("]")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unquotedValueOf() {
        assertThatThrownBy(() -> Ratings.valueOf("[A]")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownValueOf() {
        assertThatThrownBy(() -> Ratings.valueOf("[\"SOME_UNKNOWN_VALUE\"]"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equality() {
        final Ratings r1 = Ratings.of(Rating.A, Rating.B);
        assertThat(r1)
                .isSameAs(r1)
                .isEqualTo(r1)
                .isNotEqualTo(null)
                .isNotEqualTo(this.getClass());
        final Ratings r2 = Ratings.of(Rating.A, Rating.B);
        assertSoftly(softly -> {
            softly.assertThat(r1).isNotSameAs(r2).isEqualTo(r2);
            softly.assertThat(r2).isEqualTo(r1);
        });
    }
}
