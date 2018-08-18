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

package com.github.robozonky.common.remote;

import java.io.File;
import java.util.Objects;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;

class ExportApi {

    private final Supplier<ZonkyApiToken> token;

    public ExportApi(final Supplier<ZonkyApiToken> token) {
        this.token = token;
    }

    private ZonkyApiToken getToken() {
        final ZonkyApiToken result = token.get();
        final String actualScope = result.getScope();
        final String requiredScope = ZonkyApiToken.SCOPE_FILE_DOWNLOAD_STRING;
        if (!Objects.equals(actualScope, requiredScope)) {
            throw new IllegalStateException("OAuth token scoped to " + actualScope + " instead of " + requiredScope);
        }
        return result;
    }

    public File wallet() {
        final ZonkyApiToken token = getToken();
        return Export.WALLET.download(token);
    }

    public File investments() {
        final ZonkyApiToken token = getToken();
        return Export.INVESTMENTS.download(token);
    }
}
