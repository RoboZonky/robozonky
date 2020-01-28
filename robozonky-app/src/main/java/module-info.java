/*
 * Copyright 2020 The RoboZonky Project
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

module com.github.robozonky.app {
    requires java.ws.rs;
    requires java.xml;
    requires info.picocli;
    requires org.apache.logging.log4j;
    requires com.github.robozonky.api;
    requires resteasy.core;

    provides JobService with com.github.robozonky.app.version.VersionDetectionJobService,
            com.github.robozonky.app.daemon.SellingJobService,
            com.github.robozonky.app.daemon.ReservationsJobService,
            com.github.robozonky.app.delinquencies.DelinquencyNotificationJobService,
            com.github.robozonky.app.summaries.SummarizerJobService,
            com.github.robozonky.app.daemon.SaleCheckJobService;

    opens com.github.robozonky.app to info.picocli;
}
