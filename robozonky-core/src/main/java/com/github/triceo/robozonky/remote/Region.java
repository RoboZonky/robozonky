/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.remote;

/**
 * {@link #UNKNOWN} must always come last - it is an internal value, not in the Zonky API, and therefore must only get
 * its integer ID after all other values already got one.
 */
public enum Region {

    PRAHA, STREDOCESKY, JIHOCESKY, PLZENSKY, KARLOVARSKY, USTECKY, LIBERECKY, KRALOVEHRADECKY, PARDUBICKY, VYSOCINA,
    JIHOMORAVSKY, OLOMOUCKY, MORAVSKOSLEZSKY, ZLINSKY, UNKNOWN;
}
