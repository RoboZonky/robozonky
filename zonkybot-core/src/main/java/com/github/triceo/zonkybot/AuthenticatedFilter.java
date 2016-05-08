/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.zonkybot;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;

import com.github.triceo.zonkybot.remote.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AuthenticatedFilter extends CommonFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedFilter.class);

    private final Token authorization;

    public AuthenticatedFilter(final Token token) {
        authorization = token;
        AuthenticatedFilter.LOGGER.debug("Using Zonky access token '{}'.", authorization.getAccessToken());
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void filter(final ClientRequestContext clientRequestContext) throws IOException {
        clientRequestContext.getHeaders().add("Authorization", "Bearer " + authorization.getAccessToken());
        super.filter(clientRequestContext);
    }
}
