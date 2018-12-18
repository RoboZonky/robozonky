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

package com.github.robozonky.common.tenant;

/**
 * Brings the ability of {@link #getState(Class)} to only persist all changes to internal state when {@link #commit()}
 * is called. All unpersisted changes can be rolled back via {@link #abort()}. {@link #close()} will throw an
 * {@link IllegalStateException} unless {@link #commit()} or {@link #abort()} is called beforehand - this is to prevent
 * the applications from leaving uncommitted data in there.
 * <p>
 * Every method not related to state will be delegated to the underlying {@link Tenant}.
 */
public interface TransactionalTenant extends Tenant {

    void commit();

    void abort();
}
