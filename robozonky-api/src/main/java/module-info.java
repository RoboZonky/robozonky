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

import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.api.strategies.StrategyService;
import com.github.robozonky.internal.jobs.JobService;
import com.github.robozonky.internal.state.StateCleanerJobService;

module com.github.robozonky.api {
    requires java.management;
    requires java.ws.rs;
    requires java.xml.bind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.vavr;
    requires org.apache.logging.log4j;
    requires ini4j;
    requires paging.streams;
    requires resteasy.client.api;
    requires jdk.unsupported; // For sun.misc.unsafe, required by Log4J's async logging.

    /*
     * Public APIs are available for everyone.
     */
    exports com.github.robozonky.api;
    exports com.github.robozonky.api.notifications;
    exports com.github.robozonky.api.remote;
    exports com.github.robozonky.api.remote.entities;
    exports com.github.robozonky.api.remote.enums;
    exports com.github.robozonky.api.strategies;
    /*
     * Internal APIs should only ever shared to other known RoboZonky modules, unless otherwise stated. We do not want
     * to maintain API compatibility with for other implementors.
     */
    exports com.github.robozonky.internal;
    exports com.github.robozonky.internal.async;
    exports com.github.robozonky.internal.jobs;
    exports com.github.robozonky.internal.management;
    exports com.github.robozonky.internal.remote;
    exports com.github.robozonky.internal.secrets;
    exports com.github.robozonky.internal.state;
    exports com.github.robozonky.internal.tenant;
    exports com.github.robozonky.internal.test;
    exports com.github.robozonky.internal.util;
    /*
     * Extensions are managed by app and cli modules. Notifications are added as there is a slightly hackish
     * implementation of config sharing that wouldn't otherwise be possible.
     */
    exports com.github.robozonky.internal.extensions to
            com.github.robozonky.app,
            com.github.robozonky.cli,
            com.github.robozonky.notifications;
    /*
     * For the purposes of testing notification generation. We do not want any other code to create mutable entities.
     */
    exports com.github.robozonky.internal.remote.entities to
            com.github.robozonky.notifications,
            com.github.robozonky.app;

    uses JobService;
    uses StrategyService;
    uses ListenerService;

    provides JobService with StateCleanerJobService;

    opens com.github.robozonky.api.remote.entities to
            java.xml.bind,
            com.fasterxml.jackson.databind;
    opens com.github.robozonky.api.remote.enums to
            com.fasterxml.jackson.databind;

}
