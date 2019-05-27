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

import com.github.robozonky.internal.api.jobs.JobService;
import com.github.robozonky.internal.api.state.StateCleanerJobService;

module com.github.robozonky.api {
    requires java.management;
    requires java.ws.rs;
    requires java.xml.bind;
    requires jdk.jfr;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.vavr;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;
    requires ini4j;
    requires paging.streams;
    requires resteasy.client.api;

    exports com.github.robozonky.api;
    exports com.github.robozonky.api.confirmations;
    exports com.github.robozonky.api.notifications;
    exports com.github.robozonky.api.remote;
    exports com.github.robozonky.api.remote.entities;
    exports com.github.robozonky.api.remote.entities.sanitized;
    exports com.github.robozonky.api.remote.enums;
    exports com.github.robozonky.api.strategies;
    exports com.github.robozonky.internal.api;
    exports com.github.robozonky.internal.api.async;
    exports com.github.robozonky.internal.api.extensions;
    exports com.github.robozonky.internal.api.jobs;
    exports com.github.robozonky.internal.api.management;
    exports com.github.robozonky.internal.api.remote;
    exports com.github.robozonky.internal.api.secrets;
    exports com.github.robozonky.internal.api.state;
    exports com.github.robozonky.internal.api.tenant;
    exports com.github.robozonky.internal.test;
    exports com.github.robozonky.internal.util;

    uses JobService;
    uses com.github.robozonky.api.strategies.StrategyService;
    uses com.github.robozonky.api.confirmations.ConfirmationProviderService;
    uses com.github.robozonky.api.notifications.ListenerService;

    provides JobService with StateCleanerJobService;

    opens com.github.robozonky.api.remote.enums to com.fasterxml.jackson.databind, org.apache.commons.lang3;
    opens com.github.robozonky.api.remote.entities to com.fasterxml.jackson.databind, org.apache.commons.lang3;
    opens com.github.robozonky.api.remote.entities.sanitized to org.apache.commons.lang3;

}
