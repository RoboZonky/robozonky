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

package com.github.robozonky.common.management;

import java.util.UUID;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoboZonkyQueryExpTest {

    @Test
    void server() {
        final RoboZonkyQueryExp e = new RoboZonkyQueryExp();
        assertThat(e.getMBeanServer()).isEmpty();
        e.setMBeanServer(mock(MBeanServer.class));
        assertThat(e.getMBeanServer()).isNotEmpty();
        e.setMBeanServer(null);
        assertThat(e.getMBeanServer()).isEmpty();
    }

    @Test
    void correctDomain() throws MalformedObjectNameException {
        final ObjectName n = new ObjectName("com.github.robozonky", "type", UUID.randomUUID().toString());
        final RoboZonkyQueryExp e = new RoboZonkyQueryExp();
        assertThat(e.apply(n)).isTrue();
    }

    @Test
    void domainTooSpecific() throws MalformedObjectNameException {
        final ObjectName n = new ObjectName("com.github.robozonky.app", "type", UUID.randomUUID().toString());
        final RoboZonkyQueryExp e = new RoboZonkyQueryExp();
        assertThat(e.apply(n)).isFalse();
    }

    @Test
    void domainNotSpecificEnough() throws MalformedObjectNameException {
        final ObjectName n = new ObjectName("com.github", "type", UUID.randomUUID().toString());
        final RoboZonkyQueryExp e = new RoboZonkyQueryExp();
        assertThat(e.apply(n)).isFalse();
    }
}
