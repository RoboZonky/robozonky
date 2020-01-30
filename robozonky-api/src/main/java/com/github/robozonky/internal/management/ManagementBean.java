/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.management;

import java.util.function.Supplier;
import javax.management.ObjectName;

import com.github.robozonky.internal.util.functional.Either;
import com.github.robozonky.internal.util.functional.Memoizer;

public final class ManagementBean<T extends BaseMBean> {

    static final String DOMAIN = "com.github.robozonky";

    private final Supplier<ObjectName> name;
    private final Supplier<T> instance;

    public ManagementBean(final Class<T> type, final Supplier<T> constructor) {
        this.name = Memoizer.memoize(() -> assembleObjectName(type).get());
        this.instance = Memoizer.memoize(constructor);
    }

    private static Either<Throwable, ObjectName> assembleObjectName(final Class<?> clz) {
        try {
            final String packageName = clz.getPackage().getName();
            final String className = clz.getSimpleName();
            return Either.right(new ObjectName(DOMAIN + ":type=" + packageName + ",name=" + className));
        } catch (Exception ex) {
            return Either.left(ex);
        }
    }

    public ObjectName getObjectName() {
        return name.get();
    }

    public T getInstance() {
        return instance.get();
    }

}
