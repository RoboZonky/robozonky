/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky.remote;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Ratings {

    public static Ratings valueOf(final String ratings) {
        // trim the surrounding []
        final String[] parts = ratings.substring(1, ratings.length() - 2).split("\\Q,\\E");
        // trim the parts and remove surrounding quotes
        final Collection<String> strings = Stream.of(parts).map(String::trim)
                .map(string -> string.substring(1, string.length() - 2)).collect(Collectors.toList());
        // convert string representations to actual instances
        final Collection<Rating> converted = strings.stream().map(Rating::valueOf).collect(Collectors.toList());
        return Ratings.of(converted);
    }

    public static Ratings of(final Rating... ratings) {
        return Ratings.of(Arrays.asList(ratings));
    }

    public static Ratings of(final Collection<Rating> ratings) {
        return new Ratings(ratings);
    }

    public static Ratings all() {
        return Ratings.of(Rating.values());
    }

    private final Set<Rating> ratings;

    private Ratings(final Collection<Rating> ratings) {
        this.ratings = EnumSet.copyOf(ratings);
    }

    public Set<Rating> getRatings() {
        return Collections.unmodifiableSet(ratings);
    }

    @Override
    public String toString() {
        return ratings.stream().collect(Collectors.mapping(Rating::name, Collectors.joining("\", \"", "[\"", "\"]")));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ratings ratings1 = (Ratings) o;
        return Objects.equals(ratings, ratings1.ratings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ratings);
    }
}
