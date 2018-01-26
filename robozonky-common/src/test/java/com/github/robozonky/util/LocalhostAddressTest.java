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

package com.github.robozonky.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LocalhostAddressTest {

    @Test
    void isSubmitted() {
        assertThat(Scheduler.inBackground().isSubmitted(LocalhostAddress.INSTANCE)).isTrue();
    }

    @Test
    void hasValue() {
        final LocalhostAddress address = new LocalhostAddress();
        address.run();
        assertThat(address.get()).isPresent();
    }
}
