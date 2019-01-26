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

package com.github.robozonky.integrations.zonkoid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.github.robozonky.internal.api.Defaults;
import org.apache.http.HttpEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class Util {

    private static final Logger LOGGER = LogManager.getLogger(Util.class);

    private Util() {
        // no instances
    }

    public static String readEntity(final HttpEntity entity) {
        if (entity == null) {
            LOGGER.debug("Zonkoid sent an empty response.");
            return null;
        }
        final ByteArrayOutputStream outstream = new ByteArrayOutputStream(); // no need to close this one
        try {
            entity.writeTo(outstream);
            return outstream.toString(Defaults.CHARSET.displayName());
        } catch (final IOException ex) {
            LOGGER.debug("Failed reading Zonkoid response.", ex);
            return null;
        }
    }

    public static boolean isHttpSuccess(final int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}
