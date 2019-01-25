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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OAuthScopes {

    private static final String SEPARATOR = " ";
    private final Set<OAuthScope> scopes;

    private OAuthScopes(final Collection<OAuthScope> scopes) {
        this.scopes = scopes.isEmpty() ? Collections.emptySet() : EnumSet.copyOf(scopes);
    }

    public static OAuthScopes valueOf(final String input) {
        final String toParse = input.trim();
        if (toParse.length() == 0) {
            return new OAuthScopes(Collections.emptySet());
        }
        final String[] scopes = toParse.split(SEPARATOR);
        final Set<OAuthScope> result = Arrays.stream(scopes).map(OAuthScope::findByCode).collect(Collectors.toSet());
        return new OAuthScopes(result);
    }

    public static OAuthScopes of(final OAuthScope... scopes) {
        return OAuthScopes.of(Arrays.asList(scopes));
    }

    public static OAuthScopes of(final Collection<OAuthScope> scopes) {
        return new OAuthScopes(scopes);
    }

    public Set<OAuthScope> getOAuthScopes() {
        return scopes;
    }

    /**
     * Primary scope is the scope that is actually useful for the application. When {@link OAuthScope#SCOPE_APP_WEB} is
     * requested, Zonky may return scopes such as "SCOPE_APP_WEB SCOPE_APP_OAUTH" and out of the two, we only care about
     * the one that was requested.
     * @return
     */
    public Optional<OAuthScope> getPrimaryScope() {
        if (scopes.contains(OAuthScope.SCOPE_APP_WEB)) {
            return Optional.of(OAuthScope.SCOPE_APP_WEB);
        } else if (scopes.contains(OAuthScope.SCOPE_FILE_DOWNLOAD)) {
            return Optional.of(OAuthScope.SCOPE_FILE_DOWNLOAD);
        } else if (scopes.size() == 1) {
            final OAuthScope s = scopes.toArray(new OAuthScope[0])[0];
            return Optional.of(s);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return scopes.stream().map(OAuthScope::name).collect(Collectors.joining(SEPARATOR));
    }
}
