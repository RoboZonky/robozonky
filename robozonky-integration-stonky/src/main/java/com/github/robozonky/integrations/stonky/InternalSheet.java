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

package com.github.robozonky.integrations.stonky;

enum InternalSheet {

    PEOPLE("People", 1),
    WALLET("Wallet", 2),
    WELCOME("Welcome", 0);

    private final int order;
    private final String id;

    InternalSheet(final String id, final int order) {
        this.id = id;
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public String getId() {
        return id;
    }
}
