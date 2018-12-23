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
 * Contains various utilities for facilitating the daemon's asynchronous and background activities. The following
 * classes are particularly noteworthy:
 *
 * <ul>
 * <li>{@link com.github.robozonky.common.async.Backoff} implements a mechanism to repeat a remote operation
 * over and over until it is eventually successful.</li>
 * <li>{@link com.github.robozonky.common.async.Refreshable} provides a variable that automatically refreshes
 * itself from a remote operation on the background.</li>
 * <li>{@link com.github.robozonky.common.async.Reloadable} provides a variable that refreshes itself from a remote
 * operation when accessed.</li>
 * </ul>
 */
package com.github.robozonky.common.async;
