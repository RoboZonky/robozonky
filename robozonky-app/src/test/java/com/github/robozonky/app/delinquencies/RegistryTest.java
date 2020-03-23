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

package com.github.robozonky.app.delinquencies;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.github.robozonky.test.mock.MockInvestmentBuilder;

class RegistryTest extends AbstractRoboZonkyTest {

    private final Zonky zonky = harmlessZonky();
    private final Tenant tenant = mockTenant(zonky);
    private final Investment i = MockInvestmentBuilder.fresh()
        .build();

    @Test
    void persists() {
        final Registry r = new Registry(tenant);
        assertThat(r.isInitialized()).isFalse();
        assertThat(r.getCategories(i)).isEmpty();
        r.addCategory(i, Category.NEW);
        assertThat(r.getCategories(i)).isEmpty(); // nothing was persisted yet
        r.persist();
        assertThat(r.isInitialized()).isTrue();
        assertThat(r.getCategories(i)).containsOnly(Category.NEW); // was persisted now
        final Registry r2 = new Registry(tenant);
        assertThat(r2.getCategories(i)).containsOnly(Category.NEW); // was persisted permanently
    }

    @Test
    void addsAlsoLesserCategories() {
        final Registry r = new Registry(tenant);
        r.addCategory(i, Category.HOPELESS);
        r.persist();
        assertThat(r.getCategories(i))
            .containsExactly(Category.NEW, Category.MILD, Category.SEVERE, Category.CRITICAL, Category.HOPELESS);
    }

    @Test
    void defaultAddsAllCategories() {
        final Registry r = new Registry(tenant);
        r.addCategory(i, Category.DEFAULTED);
        r.persist();
        assertThat(r.getCategories(i))
            .containsExactly(Category.NEW, Category.MILD, Category.SEVERE, Category.CRITICAL, Category.HOPELESS,
                    Category.DEFAULTED);
    }

    @Test
    void removes() {
        final Registry r = new Registry(tenant);
        r.addCategory(i, Category.DEFAULTED);
        r.persist();
        assumeThat(r.getCategories(i)).isNotEmpty();
        r.remove(i);
        r.persist();
        assertThat(r.isInitialized()).isTrue();
        assertThat(r.getCategories(i)).isEmpty();
    }

    @Test
    void complements() {
        when(zonky.getInvestment(eq(i.getId()))).thenReturn(Optional.of(i));
        final Registry r = new Registry(tenant);
        r.addCategory(i, Category.DEFAULTED);
        r.persist();
        assertThat(r.complement(Collections.emptySet())).containsExactly(i);
        assertThat(r.complement(Collections.singleton(MockInvestmentBuilder.fresh()
            .build()))).containsExactly(i);
        assertThat(r.complement(Collections.singleton(i))).isEmpty();
    }
}
