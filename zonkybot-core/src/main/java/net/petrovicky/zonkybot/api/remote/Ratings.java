package net.petrovicky.zonkybot.api.remote;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class Ratings {

    public static Ratings of(Rating... ratings) {
        return Ratings.of(Arrays.asList(ratings));
    }

    public static Ratings of(List<Rating> ratings) {
        return new Ratings(ratings);
    }

    public static Ratings all() {
        return Ratings.of(Rating.values());
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
