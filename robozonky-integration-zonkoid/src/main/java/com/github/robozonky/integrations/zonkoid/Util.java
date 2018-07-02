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

package com.github.robozonky.integrations.zonkoid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.github.robozonky.internal.api.Defaults;
import org.apache.http.HttpEntity;

final class Util {

    private Util() {
        // no instances
    }

    public static String readEntity(final HttpEntity entity) {
        final ByteArrayOutputStream outstream = new ByteArrayOutputStream(); // no need to close this one
        try {
            entity.writeTo(outstream);
            return outstream.toString(Defaults.CHARSET.displayName());
        } catch (final IOException e) { // don't even log the exception as it's entirely uninteresting
            return null;
        }
    }

    public static boolean isHttpSuccess(final int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}
