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

package com.github.triceo.robozonky.api.remote.enums;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Ratings {

    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final Pattern COMMA = Pattern.compile("\\Q,\\E");

    public static Ratings valueOf(final String ratings) {
        // trim the surrounding []
        final String trimmed = Ratings.WHITESPACE.matcher(ratings).replaceAll("");
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Expecting string in the format of [\"A\",\"B\"], got " + ratings);
        } else if (trimmed.length() == 2) { // only contains []
            return Ratings.of();
        }
        final String[] parts = Ratings.COMMA.split(trimmed.substring(1, trimmed.length() - 1));
        // get the list of ratings represented by the parts of the string
        return Ratings.of(Arrays.stream(parts)
                                  .filter(part -> {
                                      if (!part.startsWith("\"") && !part.endsWith("\"") && part.length() < 3) {
                                          throw new IllegalArgumentException(
                                                  "Expecting part of string to be quoted, got " + part);
                                      }
                                      return true;
                                  }).map(part -> part.substring(1, part.length() - 1)) // remove surrounding quotes
                                  .map(Rating::valueOf) // convert string representations to actual instances
                                  .toArray(Rating[]::new));
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
        this.ratings = ratings.isEmpty() ? Collections.emptySet() : EnumSet.copyOf(ratings);
    }

    public Set<Rating> getRatings() {
        return Collections.unmodifiableSet(ratings);
    }

    @Override
    public String toString() {
        return ratings.stream().map(Rating::name).collect(Collectors.joining("\", \"", "[\"", "\"]"));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Ratings ratings1 = (Ratings) o;
        return Objects.equals(ratings, ratings1.ratings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ratings);
    }
}
