/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.common.remote;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;

/**
 * Remove when https://issues.jboss.org/browse/RESTEASY-1075 has a proper fix.
 */
final class RedirectingHttpClient extends ApacheHttpClient43Engine {

    public RedirectingHttpClient(final HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    protected void loadHttpMethod(final ClientInvocation request, final HttpRequestBase httpMethod)
            throws Exception {
        super.loadHttpMethod(request, httpMethod);
        httpMethod.setConfig(RequestConfig.copy(httpMethod.getConfig()).setRedirectsEnabled(true).build());
    }
}
