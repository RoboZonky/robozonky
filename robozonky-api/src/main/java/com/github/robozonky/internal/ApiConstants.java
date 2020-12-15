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

package com.github.robozonky.internal;

public final class ApiConstants {

    public static final String ZONKY_API_HOSTNAME = "https://api.zonky.cz";
    public static final String ROOT = "";
    public static final String OAUTH = ROOT + "/oauth";
    public static final String ME = ROOT + "/users/me";
    public static final String INVESTMENTS = ROOT + "/me/investments";
    public static final String LOANS = ROOT + "/loans";
    public static final String MARKETPLACE = LOANS + "/marketplace";
    public static final String RESERVATIONS = ROOT + "/reservations";
    public static final String RESERVATION_PREFERENCES = RESERVATIONS + "/settings";
    public static final String SMP_INVESTMENTS = ROOT + "/smp/investments";

    private ApiConstants() {
        // no instances
    }
}
