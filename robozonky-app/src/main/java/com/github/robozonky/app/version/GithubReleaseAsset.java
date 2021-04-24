/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.app.version;

import java.net.URL;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbProperty;

import com.github.robozonky.api.notifications.ReleaseAsset;

public final class GithubReleaseAsset implements ReleaseAsset {

    private String name;
    @JsonbProperty("browser_download_url")
    private URL downloadUrl;
    @JsonbProperty("size")
    private long sizeInBytes;
    @JsonbProperty("download_count")
    private int downloadCount;

    public GithubReleaseAsset() {
        // JSON-B.
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public URL getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(final URL downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    @Override
    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(final long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    @Override
    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(final int downloadCount) {
        this.downloadCount = downloadCount;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GithubReleaseAsset.class.getSimpleName() + "[", "]")
            .add("name='" + name + "'")
            .toString();
    }
}
