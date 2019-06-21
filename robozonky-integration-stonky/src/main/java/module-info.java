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

import com.github.robozonky.internal.jobs.JobService;

module com.github.robozonky.integration.stonky {
    requires java.ws.rs;
    requires google.api.client;
    requires google.api.services.drive.v3.rev153;
    requires google.api.services.sheets.v4.rev565;
    requires google.http.client;
    requires google.http.client.jackson2;
    requires google.oauth.client;
    requires google.oauth.client.java6;
    requires google.oauth.client.jetty;
    requires io.vavr;
    requires org.apache.logging.log4j;
    requires com.github.robozonky.api;

    opens com.github.robozonky.integrations.stonky to org.apache.commons.lang3;

    exports com.github.robozonky.integrations.stonky;

    provides JobService with com.github.robozonky.integrations.stonky.StonkyJobService;
}
