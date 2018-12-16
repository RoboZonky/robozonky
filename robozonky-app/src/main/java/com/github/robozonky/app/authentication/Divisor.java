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

package com.github.robozonky.app.authentication;

import java.util.concurrent.atomic.LongAdder;

final class Divisor {

    private final long max;
    private final LongAdder adder = new LongAdder();

    public Divisor(final long max) {
        this.max = max;
    }

    public void add(final long number) {
        adder.add(number);
    }

    public long getSharePerMille() {
        if (max < 1) {
            return Long.MAX_VALUE;
        }
        return (adder.sum() * 1000) / max;
    }

}
