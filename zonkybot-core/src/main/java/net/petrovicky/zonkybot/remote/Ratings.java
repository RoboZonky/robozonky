/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.petrovicky.zonkybot.remote;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Ratings {

    public static Ratings of(Rating... ratings) {
        return of(Arrays.asList(ratings));
    }

    public static Ratings of(List<Rating> ratings) {
        return new Ratings(ratings);
    }

    public static Ratings all() {
        return of(Rating.values());
    }

    private final Set<Rating> ratings = new LinkedHashSet<>();

    private Ratings(Collection<Rating> ratings) {
        this.ratings.addAll(ratings);
    }

    public Set<Rating> getRatings() {
        return Collections.unmodifiableSet(this.ratings);
    }

    @Override
    public String toString() {
        return ratings.stream().collect(Collectors.mapping(Rating::name, Collectors.joining(", ", "[", "]")));
    }
}
