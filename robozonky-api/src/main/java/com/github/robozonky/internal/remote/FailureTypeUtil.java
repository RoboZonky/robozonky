/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.remote;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class FailureTypeUtil {

    private static final Logger LOGGER = LogManager.getLogger(FailureTypeUtil.class);

    private FailureTypeUtil() {
        // no external instances
    }

    public static String getResponseEntity(final Response response) {
        if (!response.hasEntity()) {
            return "";
        }
        response.bufferEntity(); // allow for repeated queries over the same Response instance
        final String contents = response.readEntity(String.class);
        LOGGER.debug("Response body is: {}", contents);
        return contents;
    }

    public static boolean matches(final Class<? extends ClientErrorException> expectedException,
            final ClientErrorException actualException,
            final String reason) {
        if (!actualException.getClass()
            .isAssignableFrom(expectedException)) {
            return false;
        } else if (reason == null) {
            return true;
        } else {
            return FailureTypeUtil.getResponseEntity(actualException.getResponse())
                .contains(reason);
        }
    }

}
