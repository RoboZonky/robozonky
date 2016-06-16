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

package com.github.triceo.robozonky.remote;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ZotifyApiTest {

    private static ResteasyClient CLIENT;
    private static ZotifyApi API;

    @Test
    public void testBasicParsing() {
        ZotifyApiTest.API.getLoans();
    }

    @BeforeClass
    public static void startUp() {
        ZotifyApiTest.CLIENT = new ResteasyClientBuilder().build();
        ZotifyApiTest.API = ZotifyApiTest.CLIENT.target("http://zotify.cz").proxy(ZotifyApi.class);
    }

    @AfterClass
    public static void cleanUp() {
        ZotifyApiTest.CLIENT.close();
    }

}
