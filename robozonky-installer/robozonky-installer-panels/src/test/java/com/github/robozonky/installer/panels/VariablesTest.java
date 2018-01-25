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

package com.github.robozonky.installer.panels;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.izforge.izpack.api.data.InstallData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class VariablesTest {

    @Test
    public void unique() {
        final Set<String> values = Stream.of(Variables.values())
                .map(Variables::getKey)
                .collect(Collectors.toSet());
        Assertions.assertThat(values).hasSameSizeAs(Variables.values());
    }

    @Test
    public void readValue() {
        for (final Variables variable : Variables.values()) {
            final String key = variable.getKey();
            final String value = UUID.randomUUID().toString();
            final InstallData data = Mockito.mock(InstallData.class);
            Mockito.when(data.getVariable(key)).thenReturn(value);
            Assertions.assertThat(variable.getValue(data)).isSameAs(value);
        }
    }
}
