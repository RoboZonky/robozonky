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

package com.github.robozonky.api;

/**
 * Uniquely identifies the Zonky user that the application is working on behalf of, and carries some Zonky-imposed
 * restrictions that need to be enforced on the session.
 */
public interface SessionInfo {

    /**
     * Whether or not the robot is doing a dry run. Dry run means that no portfolio-altering operations will be
     * performed, even though the robot would still continue doing everything else.
     *
     * @return True if the robot is doing a dry run.
     */
    boolean isDryRun();

    /**
     * @return Zonky username of the current user.
     */
    String getUsername();

    /**
     * @return Name of the robot session currently running.
     */
    String getName();

    boolean canInvest();

    boolean canAccessSmp();

    Money getMinimumInvestmentAmount();

    Money getInvestmentStep();

    Money getMaximumInvestmentAmount();

}
