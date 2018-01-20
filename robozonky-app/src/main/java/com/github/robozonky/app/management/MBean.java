/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.management;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.github.robozonky.app.runtime.RuntimeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum MBean {

    RUNTIME(Runtime.class) {
        @Override
        BaseMBean newImplementation(final RuntimeHandler runtimeHandler) {
            return new Runtime(runtimeHandler);
        }
    },
    OPERATIONS(Operations.class) {
        @Override
        BaseMBean newImplementation(final RuntimeHandler runtimeHandler) {
            return new Operations();
        }
    },
    DELINQUENCY(Delinquency.class) {
        @Override
        BaseMBean newImplementation(final RuntimeHandler runtimeHandler) {
            return new Delinquency();
        }
    },
    PORTFOLIO(Portfolio.class) {
        @Override
        BaseMBean newImplementation(final RuntimeHandler runtimeHandler) {
            return new Portfolio();
        }
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(MBean.class);
    private final ObjectName objectName;

    MBean(final Class<? extends BaseMBean> implClass) {
        this.objectName = MBean.assembleObjectName(implClass);
    }

    static ObjectName assembleObjectName(final Class<? extends BaseMBean> implementation) {
        try {
            final String className = implementation.getSimpleName();
            return new ObjectName("com.github.robozonky:type=" + className);
        } catch (final MalformedObjectNameException ex) {
            MBean.LOGGER.warn("MBean '{}' will be ignored.", implementation, ex);
            return null;
        }
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    abstract BaseMBean newImplementation(final RuntimeHandler runtimeHandler);

}
