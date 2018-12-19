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

package com.github.robozonky.app.delinquencies;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.common.tenant.Tenant;

final class Registry {

    private final Tenant tenant;
    private final Map<Category, Storage> storages;

    public Registry(final Tenant tenant) {
        this.tenant = tenant;
        final Map<Category, Storage> tmp = new EnumMap<>(Category.class);
        Arrays.stream(Category.values()).forEach(cat -> tmp.put(cat, new Storage(tenant, getId(cat))));
        this.storages = Collections.unmodifiableMap(tmp);
    }

    private static String getId(final Category category) {
        if (category.getThresholdInDays() < 0) {
            return "defaulted";
        } else {
            return "delinquent" + category.getThresholdInDays() + "plus";
        }
    }

    private static long getId(final Investment investment) {
        return investment.getId();
    }

    public boolean isInitialized() {
        return tenant.getState(Storage.class).isInitialized();
    }

    public Collection<Investment> complement(final Collection<Investment> investments) {
        final Set<Long> idsToComplement = investments.stream().map(Registry::getId).collect(Collectors.toSet());
        return storages.get(Category.NEW).complement(idsToComplement)
                .parallel()
                .mapToObj(id -> tenant.call(z -> z.getInvestment(id)))
                .flatMap(i -> i.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toList());
    }

    public EnumSet<Category> getCategories(final Investment investment) {
        final long id = getId(investment);
        final Set<Category> result = storages.entrySet().stream()
                .filter(e -> e.getValue().isKnown(id))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        return result.isEmpty() ? EnumSet.noneOf(Category.class) : EnumSet.copyOf(result);
    }

    private void addToCategory(final Category category, final long id) {
        storages.get(category).add(id);
    }

    public void addCategory(final Investment investment, final Category category) {
        final long id = getId(investment);
        final Set<Category> categories = category == Category.DEFAULTED ?
                storages.keySet() :
                EnumSet.of(category, category.getLesser().toArray(Category[]::new));
        categories.forEach(cat -> addToCategory(cat, id));
    }

    public void remove(final Investment investment) {
        storages.values().forEach(storage -> storage.remove(getId(investment)));
    }

    public void persist() {
        storages.values().forEach(Storage::persist);
    }
}
