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

import java.util.List;

import org.assertj.core.api.Assertions;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ZonkyApiTest {

    private static ResteasyClient CLIENT;
    private static ZonkyApi API;

    @Test
    public void testBasicParsing() {
        final Ratings ratings = Ratings.all();
        final List<Loan> loans = ZonkyApiTest.API.getLoans(ratings, 200);
        Assertions.assertThat(loans).isNotEmpty();
        Assertions.assertThat(loans.get(0).getRemainingInvestment()).isGreaterThan(0);
    }

    @BeforeClass
    public static void startUp() {
        ZonkyApiTest.CLIENT = new ResteasyClientBuilder().build();
        ZonkyApiTest.API = ZonkyApiTest.CLIENT.target("https://api.zonky.cz").proxy(ZonkyApi.class);
    }

    @AfterClass
    public static void cleanUp() {
        ZonkyApiTest.CLIENT.close();
    }

}
