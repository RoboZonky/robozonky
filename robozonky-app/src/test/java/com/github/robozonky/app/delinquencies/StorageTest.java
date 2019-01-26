/*
 * Copyright 2019 The RoboZonky Project
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

import java.util.Collections;
import java.util.UUID;

import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.assumeThat;

class StorageTest extends AbstractRoboZonkyTest {

    private final Zonky zonky = harmlessZonky(10_000);
    private final Tenant tenant = mockTenant(zonky);

    @Test
    void persists() {
        final String uid = UUID.randomUUID().toString();
        final long id = 1;
        final Storage s = new Storage(tenant, uid);
        s.add(id);
        s.persist();
        final Storage s2 = new Storage(tenant, uid);
        assertThat(s2.isKnown(id)).isTrue();
    }

    @Test
    void doesNotAddTwice() {
        final long id = 1;
        final Storage s = new Storage(tenant, UUID.randomUUID().toString());
        assertThat(s.add(id)).isTrue();
        assertThat(s.add(id)).isFalse();
        assertThat(s.remove(id)).isFalse(); // the original did not contain this id
        s.persist();
        assertThat(s.isKnown(id)).isFalse(); // the add call was negated by the remove call
    }

    @Test
    void complementsWhenEmpty() {
        final long id = 1;
        final Storage s = new Storage(tenant, UUID.randomUUID().toString());
        assertThat(s.complement(Collections.emptySet())).isEmpty();
        assertThat(s.complement(Collections.singleton(id))).isEmpty();
    }

    @Test
    void complementsWhenHasValue() {
        final long id = 1;
        final Storage s = new Storage(tenant, UUID.randomUUID().toString());
        s.add(id);
        s.persist();
        // start the test
        assertThat(s.complement(Collections.emptySet())).containsOnly(id);
        assertThat(s.complement(Collections.singleton(2l))).containsOnly(id);
        assertThat(s.complement(Collections.singleton(id))).isEmpty();
    }

    @Test
    void doesNotRemoveTwice() {
        final long id = 1;
        final Storage s = new Storage(tenant, UUID.randomUUID().toString());
        s.add(id);
        s.persist();
        // now start the test
        assertThat(s.remove(id)).isTrue();
        assertThat(s.remove(id)).isFalse();
        assertThat(s.add(id)).isFalse(); // the original contained this id
        s.persist();
        assertThat(s.isKnown(id)).isTrue(); // the remove call was negated by the add call
    }

    @Test
    void onlyChangesWhenPersisted() {
        final long id = 1;
        final Storage s = new Storage(tenant, UUID.randomUUID().toString());
        assumeThat(s.isKnown(id)).isFalse();
        s.add(id);
        assertThat(s.isKnown(id)).isFalse();
        s.persist();
        assertThat(s.isKnown(id)).isTrue();
        s.remove(id);
        assertThat(s.isKnown(id)).isTrue();
        s.persist();
        assertThat(s.isKnown(id)).isFalse();
    }

}
