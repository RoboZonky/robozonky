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

package com.github.robozonky.notifications.samples;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import com.github.robozonky.api.notifications.Release;
import com.github.robozonky.api.notifications.ReleaseAsset;
import com.github.robozonky.internal.test.DateUtil;

final class SampleRelease implements Release {

    private final boolean prerelease;

    public SampleRelease(boolean prerelease) {
        this.prerelease = prerelease;
    }

    @Override
    public URL getUrl() {
        try {
            return new URL("http://robozonky.github.io");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getName() {
        return "RoboZonky Sample Release";
    }

    @Override
    public ZonedDateTime getDatePublished() {
        return DateUtil.zonedNow();
    }

    @Override
    public boolean isPrerelease() {
        return prerelease;
    }

    @Override
    public List<? extends ReleaseAsset> getAssets() {
        return Collections.singletonList(new ReleaseAsset() {
            @Override
            public String getName() {
                return "Sample release asset.";
            }

            @Override
            public URL getDownloadUrl() {
                return getUrl();
            }

            @Override
            public long getSizeInBytes() {
                return 123_456_789;
            }

            @Override
            public int getDownloadCount() {
                return 1_234;
            }
        });
    }
}
