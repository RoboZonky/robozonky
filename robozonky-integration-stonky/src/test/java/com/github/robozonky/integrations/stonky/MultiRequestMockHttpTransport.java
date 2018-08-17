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

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;

public class MultiRequestMockHttpTransport extends MockHttpTransport {

    private final Set<ResponseHandler> responseHandlers = new LinkedHashSet<>(0);

    private static LowLevelHttpRequest getRequest(final MockLowLevelHttpResponse response) {
        final MockLowLevelHttpRequest request = new MockLowLevelHttpRequest();
        request.setResponse(response);
        return request;
    }

    public void addReponseHandler(final ResponseHandler handler) {
        responseHandlers.add(handler);
    }

    @Override
    public LowLevelHttpRequest buildRequest(final String method, final String url) throws IOException {
        return responseHandlers.stream()
                .map(h -> h.apply(method, url))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .findFirst()
                .map(MultiRequestMockHttpTransport::getRequest)
                .orElse(super.buildRequest(method, url));
    }
}
