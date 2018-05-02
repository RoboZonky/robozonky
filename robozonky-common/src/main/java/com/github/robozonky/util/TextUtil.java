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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.robozonky.internal.api.Defaults;

public class TextUtil {

    public static <T> String toString(final Collection<T> items, final Function<T, String> converter) {
        return items.stream()
                .map(converter)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    public static Optional<String> md5(final String secret) {
        try {
            final MessageDigest mdEnc = MessageDigest.getInstance("MD5");
            mdEnc.update(secret.getBytes(Defaults.CHARSET));
            return Optional.of(new BigInteger(1, mdEnc.digest()).toString(16));
        } catch (final Exception ex) {
            return Optional.empty();
        }
    }

}
