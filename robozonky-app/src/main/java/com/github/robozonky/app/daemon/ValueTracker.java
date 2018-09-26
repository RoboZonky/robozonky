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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ValueTracker implements Supplier<BigDecimal> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValueTracker.class);

    private final AtomicReference<BigDecimal> value = new AtomicReference<>();
    private final Consumer<BigDecimal> changeListener;

    public ValueTracker(final BigDecimal original, final Consumer<BigDecimal> changeListener) {
        this.changeListener = changeListener;
        set(original);
    }

    public void add(final BigDecimal change) {
        value.accumulateAndGet(change, BigDecimal::add);
        LOGGER.debug("Locally added {} CZK, is {} CZK.", change, value.get());
        changeListener.accept(get());
    }

    public void set(final BigDecimal newValue) {
        value.set(newValue);
        LOGGER.debug("Set to {} CZK.", newValue);
        changeListener.accept(get());
    }

    @Override
    public BigDecimal get() {
        return value.get();
    }
}
