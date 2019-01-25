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

package com.github.robozonky.common.async;

import java.util.function.BooleanSupplier;

/**
 * Operations here are assumed to be atomic. A call to {@link #markReloaded(T)} or {@link #forceReload()} must be
 * immediately reflected in the value of {@link #getAsBoolean()}.
 */
interface ReloadDetection<T> extends BooleanSupplier {

    void markReloaded(final T newValue);

    void forceReload();
}
