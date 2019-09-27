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

package com.github.robozonky.test.mock;

import org.mockito.Mockito;

import java.util.Random;

abstract class BaseMockBuilder<T, S extends BaseMockBuilder<T, S>> {

    protected static final Random RANDOM = new Random();
    protected final T mock;

    protected BaseMockBuilder(Class<T> clz) {
        mock = Mockito.mock(clz);
    }

    public final T build() {
        return mock;
    }

}
