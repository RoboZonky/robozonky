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

package com.github.robozonky.cli;

import java.net.URL;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(commandNames = "notification-tester", commandDescription = NotificationTestingFeature.DESCRIPTION)
public final class NotificationTestingFeature implements Feature {

    static final String DESCRIPTION = "Send a testing notification according to the provided configuration.";

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTestingFeature.class);

    @Parameter(names = {"-u", "--username"}, description = "Zonky username.", required = true)
    private String username = null;
    @Parameter(names = {"-l", "--location"}, description = "URL leading to the strategy.", required = true)
    private URL location;
    private String text;

    public NotificationTestingFeature(final String username, final URL location) {
        this.username = username;
        this.location = location;
    }

    NotificationTestingFeature() {
        // for JCommander
    }

    @Override
    public String describe() {
        return DESCRIPTION;
    }

    @Override
    public void setup() {
        // nothing to do
    }

    @Override
    public void test() throws TestFailedException {
        final boolean success = Checker.notifications(username, location);
        if (success) {
            LOGGER.info("Notifications should have been sent now.");
        } else {
            throw new TestFailedException("No notifications have been sent. Check log for possible problems.");
        }
    }
}
