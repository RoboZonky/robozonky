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

/**
 * Provides means to persistently store information. Use
 * {@link com.github.robozonky.common.state.TenantState#of(com.github.robozonky.api.SessionInfo)} as primary entry
 * point.
 * <p>
 * This class uses two main concepts:
 *
 * <ul>
 * <li>A tenant is a single user of the application. All state is always specific to a tenant. A tenant is
 * represented by a {@link com.github.robozonky.api.SessionInfo} instance.</li>
 * <li>An instance is a {@link java.lang.Class} that is storing the state, as provided in
 * {@link com.github.robozonky.common.state.TenantState#in(java.lang.Class)}.</li>
 * </ul>
 * <p>
 * At the moment, there is only one implementation of these concepts, one backed by a file.
 * ({@link com.github.robozonky.common.state.FileBackedStateStorage}, implementing the INI file format.) Every tenant
 * gets a single file, every instance then is a section in this file. Each such section is a fairly standard property
 * file.
 * <p>
 * For backwards compatibility reasons, the classes also use
 * {@link com.github.robozonky.internal.api.Settings#getStateFile()} as an underlying read-only state storage. If a key
 * cannot be looked up in a main storage, it will be looked up in this backup storage. All write operations go
 * directly to the main storage. This is done so that RoboZonky gracefully reads old state information that was not
 * tenant-specific, but gradually replaces it as new stores are made. This is deprecated and will eventually be removed.
 */
package com.github.robozonky.common.state;
