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

package com.github.robozonky.internal.remote;

class AuthenticationFilter extends RoboZonkyFilter {

    public AuthenticationFilter() { // encoded secret
        this.setRequestHeader("Authorization", "Basic cm9ib3pvbmt5OjludEN6Y2lta0dBYXIzc2d6bXRlUVFNa3FxOHVkRg==");
    }
}
