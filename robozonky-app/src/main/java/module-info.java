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

module com.github.robozonky.app {
    requires java.ws.rs;
    requires java.xml;
    requires com.fasterxml.jackson.databind;
    requires info.picocli;
    requires io.vavr;
    requires jdk.jfr;
    requires maven.artifact;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;
    requires com.github.robozonky.api;
    requires com.github.robozonky.common;

    provides JobService with com.github.robozonky.app.events.EventFiringJobService,
            com.github.robozonky.app.version.VersionDetectionJobService,
            com.github.robozonky.app.daemon.SellingJobService,
            com.github.robozonky.app.daemon.ReservationsJobService,
            com.github.robozonky.app.delinquencies.DelinquencyNotificationJobService,
            com.github.robozonky.app.summaries.SummarizerJobService,
            com.github.robozonky.app.transactions.TransactionProcessingJobService;

    opens com.github.robozonky.app.configuration to info.picocli;
    opens com.github.robozonky.app.events.impl to org.apache.commons.lang3;
}
