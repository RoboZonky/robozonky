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

package com.github.robozonky.integrations.stonky;

import java.util.Objects;
import java.util.UUID;

import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.services.drive.model.File;

public class CopyFileResponseHandler extends ResponseHandler {

    private final File file;

    public CopyFileResponseHandler(final File file) {
        this.file = file;
    }

    @Override
    protected boolean appliesTo(final String method, final String url) {
        return Objects.equals(method, "POST") &&
                url.startsWith("https://www.googleapis.com/drive/v3/files/" + file.getId() + "/copy?");
    }

    @Override
    protected MockLowLevelHttpResponse respond(final String method, final String url) {
        final File newFile = file.clone();
        file.setId(UUID.randomUUID().toString());
        return new MockLowLevelHttpResponse().setContent(toJson(newFile));
    }
}
