/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import com.github.robozonky.internal.api.Settings;

public final class UrlUtil {

    private UrlUtil() {
        // no instances
    }

    public static InputStream open(final URL url) throws IOException {
        final URLConnection con = url.openConnection();
        con.setConnectTimeout((int) Settings.INSTANCE.getConnectionTimeout().toMillis());
        con.setReadTimeout((int) Settings.INSTANCE.getSocketTimeout().toMillis());
        return con.getInputStream();
    }
}
