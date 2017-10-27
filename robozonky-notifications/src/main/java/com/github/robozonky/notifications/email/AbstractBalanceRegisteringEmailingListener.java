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

package com.github.robozonky.notifications.email;

import java.util.function.Function;

import com.github.robozonky.api.notifications.Event;

abstract class AbstractBalanceRegisteringEmailingListener<T extends Event> extends AbstractEmailingListener<T> {

    private final Function<T, Integer> balanceSupplier;

    protected AbstractBalanceRegisteringEmailingListener(final Function<T, Integer> balanceSupplier,
                                                         final ListenerSpecificNotificationProperties properties) {
        super(properties);
        this.balanceSupplier = balanceSupplier;
        registerFinisher(event -> BalanceTracker.INSTANCE.setLastKnownBalance(balanceSupplier.apply(event)));
    }

    protected int getNewBalance(final T event) {
        return balanceSupplier.apply(event);
    }
}
