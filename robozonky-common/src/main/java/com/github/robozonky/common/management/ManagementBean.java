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

package com.github.robozonky.common.management;

import java.util.function.Supplier;
import javax.management.ObjectName;

import io.vavr.Lazy;
import io.vavr.control.Either;
import io.vavr.control.Try;

public final class ManagementBean<T extends BaseMBean> {

    private final Lazy<ObjectName> name;
    private final Lazy<T> instance;

    public ManagementBean(final Class<T> type, final Supplier<T> constructor) {
        this.name = Lazy.of(() -> assembleObjectName(type).get());
        this.instance = Lazy.of(constructor);
    }

    private static Either<Throwable, ObjectName> assembleObjectName(final Class<?> clz) {
        return Try.of(() -> {
            final String className = clz.getSimpleName();
            return new ObjectName("com.github.robozonky:type=" + className);
        }).toEither();
    }

    public ObjectName getObjectName() {
        return name.get();
    }

    public T getInstance() {
        return instance.get();
    }
}
