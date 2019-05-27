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

module com.github.robozonky.common {
    requires java.management;
    requires java.ws.rs;
    requires ini4j;
    requires io.vavr;
    requires jdk.jfr;
    requires org.apache.logging.log4j;
    requires paging.streams;
    requires resteasy.client.api;
    requires com.github.robozonky.api;

    exports com.github.robozonky.internal.api.async;
    exports com.github.robozonky.internal.api.extensions;
    exports com.github.robozonky.internal.api.jobs;
    exports com.github.robozonky.internal.api.management;
    exports com.github.robozonky.internal.api.remote;
    exports com.github.robozonky.internal.api.secrets;
    exports com.github.robozonky.internal.api.state;
    exports com.github.robozonky.internal.api.tenant;

    uses JobService;
    uses com.github.robozonky.api.strategies.StrategyService;
    uses com.github.robozonky.api.confirmations.ConfirmationProviderService;
    uses com.github.robozonky.api.notifications.ListenerService;

    provides JobService with StateCleanerJobService;
}
