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

package com.github.robozonky.common.remote;

import java.util.function.Consumer;
import java.util.function.Function;

import jdk.jfr.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Api<T> {

    private static final Logger LOGGER = LogManager.getLogger(Api.class);

    private final T proxy;

    public Api(final T proxy) {
        this.proxy = proxy;
    }

    static <Y, Z> Z call(final Function<Y, Z> function, final Y proxy) {
        LOGGER.trace("Executing...");
        final Event event = new ZonkyCallJfrEvent();
        try {
            event.begin();
            return function.apply(proxy);
        } finally {
            event.commit();
            LOGGER.trace("... done.");
        }
    }

    <S> S call(final Function<T, S> function) {
        return call(function, proxy);
    }

    void run(final Consumer<T> consumer) {
        final Function<T, Boolean> wrapper = t -> {
            consumer.accept(t);
            return false; // we need to return something
        };
        call(wrapper);
    }
}
