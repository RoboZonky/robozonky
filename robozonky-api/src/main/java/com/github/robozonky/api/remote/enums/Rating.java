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

package com.github.robozonky.api.remote.enums;

import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.robozonky.internal.api.Settings;

public enum Rating implements BaseEnum {

    // it is imperative for proper functioning of strategy algorithms that ratings here be ordered best to worst
    AAAAA("A**"),
    AAAA("A*"),
    AAA("A++"),
    AA("A+"),
    A("A"),
    B("B"),
    C("C"),
    D("D");

    private final String code;

    Rating(final String code) {
        this.code = code;
    }

    public static Rating findByCode(final String code) {
        return Stream.of(Rating.values())
                .filter(r -> Objects.equals(r.code, code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown rating: " + code));
    }

    public TemporalAmount getCaptchaDelay() {
        return Settings.INSTANCE.getCaptchaDelay(this);
    }

    @Override
    public String getCode() {
        return code;
    }

}
