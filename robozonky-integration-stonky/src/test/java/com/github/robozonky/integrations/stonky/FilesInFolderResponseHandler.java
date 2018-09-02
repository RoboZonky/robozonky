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

import com.google.api.services.drive.model.File;

public class FilesInFolderResponseHandler extends AllFilesResponseHandler {

    private final File parent;

    public FilesInFolderResponseHandler(final File parent, final File... files) {
        super(files);
        this.parent = parent;
    }

    @Override
    protected boolean appliesTo(final String method, final String url) {
        final String expected = "https://www.googleapis.com/drive/v3/files?q='" + parent.getId() + "'%20in%20parents";
        return Objects.equals(method, "GET") && Objects.equals(url, expected);
    }
}
