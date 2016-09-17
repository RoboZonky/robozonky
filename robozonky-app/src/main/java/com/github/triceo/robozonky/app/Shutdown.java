/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.util.Stack;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Shutdown {

    private static final Logger LOGGER = LoggerFactory.getLogger(Shutdown.class);

    private final Stack<Consumer<ReturnCode>> stack = new Stack<>();

    public void before(final Consumer<ReturnCode> action) {
        stack.push(action);
    }

    public void now(final ReturnCode returnCode) {
        Shutdown.LOGGER.debug("RoboZonky terminating with '{}' return code.", returnCode);
        while (!stack.isEmpty()) {
            stack.pop().accept(returnCode);
        }
        Shutdown.LOGGER.info("===== RoboZonky out. =====");
        System.exit(returnCode.getCode());
    }

}
