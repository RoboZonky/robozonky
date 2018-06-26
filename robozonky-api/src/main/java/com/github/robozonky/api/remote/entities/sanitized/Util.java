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

package com.github.robozonky.api.remote.entities.sanitized;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

import com.github.robozonky.api.remote.entities.RawLoan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Util {

    private static final Map<String, BigDecimal> BIGDECIMAL_CACHE = new WeakHashMap<>(0);
    private static final Function<Integer, String> LOAN_URL_SUPPLIER =
            (id) -> "https://app.zonky.cz/#/marketplace/detail/" + id + "/";
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private static URL toUrl(final String url) {
        try {
            return new URL(url);
        } catch (final MalformedURLException ex) {
            throw new IllegalStateException("Impossible.", ex);
        }
    }

    private static URL guessUrl(final int loanId) {
        return toUrl(LOAN_URL_SUPPLIER.apply(loanId));
    }

    /**
     * Zonky's API documentation states that {@link RawLoan#getUrl()} is optional. Therefore the only safe use of that
     * attribute is through this method.
     * @return URL to a loan on Zonky's website. Guessed if not present.
     */
    static URL getUrlSafe(final RawLoan l) {
        // in case investment has no loan, we guess loan URL
        final String providedUrl = l.getUrl();
        if (providedUrl == null) {
            return guessUrl(l.getId());
        } else {
            return toUrl(providedUrl);
        }
    }

    /**
     * Reuses a single instance of {@link BigDecimal} for most common values, saving a lot of memory.
     * @param original
     * @return
     */
    static BigDecimal cacheBigDecimal(final BigDecimal original) {
        if (original == null) {
            return null;
        }
        final String s = original.toString();
        return BIGDECIMAL_CACHE.computeIfAbsent(s, k -> original);
    }
}

