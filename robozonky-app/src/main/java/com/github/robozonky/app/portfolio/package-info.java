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
 * This package contains classes that maintain and manipulate RoboZonky's internal representation of the user's
 * portfolio. They are typically stateful and their instances are kept for the entire lifetime of the daemon.
 * <p>
 * Do not, in any way, change FQN of any of the classes in this package. It would result in losing delinquency state
 * that had potentially been accumulating for months.
 */
package com.github.robozonky.app.portfolio;
