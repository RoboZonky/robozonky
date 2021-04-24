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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbProperty;

import com.github.robozonky.api.notifications.Release;
import com.github.robozonky.internal.Defaults;

public final class GithubRelease implements Release {

    @JsonbProperty("html_url")
    private URL url;

    private String name;

    @JsonbProperty("published_at")
    private ZonedDateTime datePublished;

    private boolean draft;
    private boolean prerelease;

    private List<GithubReleaseAsset> assets;

    public GithubRelease() {
        // JSON-B.
    }

    public GithubRelease(String name, GithubReleaseAsset... assets) {
        this.name = name;
        try {
            this.url = new URL("http://robozonky.github.io/" + URLEncoder.encode(name, Defaults.CHARSET));
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        this.assets = Arrays.asList(assets);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public void setUrl(final URL url) {
        this.url = url;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public ZonedDateTime getDatePublished() {
        return datePublished;
    }

    public void setDatePublished(final ZonedDateTime datePublished) {
        this.datePublished = datePublished;
    }

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(final boolean draft) {
        this.draft = draft;
    }

    @Override
    public boolean isPrerelease() {
        return prerelease;
    }

    public void setPrerelease(final boolean prerelease) {
        this.prerelease = prerelease;
    }

    @Override
    public List<GithubReleaseAsset> getAssets() {
        return Collections.unmodifiableList(assets);
    }

    public void setAssets(final List<GithubReleaseAsset> assets) {
        this.assets = new ArrayList<>(assets);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final GithubRelease that = (GithubRelease) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GithubRelease.class.getSimpleName() + "[", "]")
            .add("name='" + name + "'")
            .toString();
    }
}
