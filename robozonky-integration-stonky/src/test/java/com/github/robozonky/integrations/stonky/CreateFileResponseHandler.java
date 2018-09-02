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
import java.util.function.Consumer;

import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.services.drive.model.File;

public class CreateFileResponseHandler extends ResponseHandler {

    private static final String ROOT = "https://www.googleapis.com/upload/drive/v3/files";
    private final Consumer<File> updater;

    public CreateFileResponseHandler(final Consumer<File> updater) {
        this.updater = updater;
    }

    @Override
    protected boolean appliesTo(final String method, final String url) { // file uploads have two parts
        return url.startsWith(ROOT) &&
                (Objects.equals(method, "PUT") || Objects.equals(method, "POST"));
    }

    @Override
    protected MockLowLevelHttpResponse respond(final String method, final String url) {
        final File file = GoogleUtil.getFile(UUID.randomUUID().toString());
        updater.accept(file); // inform other code that a new file has been created
        /*
         *
         * "location" header handles the redirect from first part to second part, otherwise API will throw NPE. the
         * value given is synthetic and has no relation to anything that Google may put there.
         */
        return new MockLowLevelHttpResponse()
                .setContent(toJson(file))
                .addHeader("Location", ROOT + "?id=" + file.getId() + "uploadType=resumable");
    }
}
