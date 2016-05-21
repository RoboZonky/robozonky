/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky.remote;

public enum Rating {

    AAAAA("A**"),
    AAAA("A*"),
    AAA("A++"),
    AA("A+"),
    A("A"),
    B("B"),
    C("C"),
    D("D");


    private final String description;

    Rating(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
