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

package com.github.robozonky.integrations.stonky;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class InternalSheetTest {

    @Test
    void uniqueIds() {
        final Set<String> ids = Stream.of(InternalSheet.values())
                .map(InternalSheet::getId)
                .collect(Collectors.toSet());
        assertThat(ids).hasSize(InternalSheet.values().length);
    }

    @Test
    void uniqueOrders() {
        final Set<Integer> ids = Stream.of(InternalSheet.values())
                .map(InternalSheet::getOrder)
                .collect(Collectors.toSet());
        assertThat(ids).hasSize(InternalSheet.values().length);
    }

}
