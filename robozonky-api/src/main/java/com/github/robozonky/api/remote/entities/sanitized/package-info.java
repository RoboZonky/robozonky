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

/**
 * Zonky API is notorious for only providing certain field values at certain stages in the
 * {@link com.github.robozonky.api.remote.entities.RawInvestment}/
 * {@link com.github.robozonky.api.remote.entities.RawLoan}/
 * {@link com.github.robozonky.api.remote.entities.RawDevelopment}/
 * {@link com.github.robozonky.api.remote.entities.RawReservation} lifecycle. The aim of this package is to provide
 * null-safe alternatives for some of the Zonky API entities. {@link java.lang.NullPointerException}s thrown from within
 * this package should be considered bugs in RoboZonky and not some quirks of the Zonky API.
 */
package com.github.robozonky.api.remote.entities.sanitized;
