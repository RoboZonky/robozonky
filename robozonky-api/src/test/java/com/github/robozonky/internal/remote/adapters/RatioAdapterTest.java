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

package com.github.robozonky.internal.remote.adapters;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Ratio;

public class RatioAdapterTest {

    private final RatioAdapter adapter = new RatioAdapter();

    @Test
    void marshalAndUnmarshal() {
        String json = adapter.adaptToJson(Ratio.fromPercentage(11.1));
        Ratio ratio = adapter.adaptFromJson(json);
        Assertions.assertThat(ratio)
            .isEqualTo(Ratio.fromRaw("0.111"));
    }

    @Test
    void marshalAndUnmarshalZero() {
        String json = adapter.adaptToJson(Ratio.ZERO);
        Ratio ratio = adapter.adaptFromJson(json);
        Assertions.assertThat(ratio)
            .isSameAs(Ratio.ZERO);
    }

}
