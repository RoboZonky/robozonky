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

package com.github.robozonky.api.confirmations;

/**
 * Remote endpoint to provide confirmations on investments.
 */
@Deprecated(forRemoval = true, since = "5.3.0")
public interface ConfirmationProvider {

    /**
     * Ask the remote endpoint for confirmation on investment. This is a blocking operation, implementors are advised
     * to return immediately.
     * @param auth Information about this RoboZonky instance.
     * @param loanId The investment to confirm.
     * @param amount The amount to confirm.
     * @return Whether or not the remote request suceeded.
     */
    boolean requestConfirmation(RequestId auth, final int loanId, final int amount);

    /**
     * Unique provider identification. When seen by users, they will understand which service this is.
     * @return Unique string identifying the service.
     */
    String getId();
}
