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
 * Contains all the logic used for creating {@link com.github.robozonky.api.notifications.Event} instances and
 * distributing them to {@link com.github.robozonky.api.notifications.EventListener}s. Refer to
 * {@link com.github.robozonky.app.events.EventFactory} in order to create
 * {@link com.github.robozonky.api.notifications.Event} instances. Call
 * {@link com.github.robozonky.app.events.Events#fire(com.github.robozonky.app.events.LazyEvent)} to fire them.
 */
package com.github.robozonky.app.events;
