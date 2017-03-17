/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications.files;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.triceo.robozonky.api.Refreshable;

class RefreshableNotificationProperties extends Refreshable<NotificationProperties> {

    @Override
    public Optional<Refreshable<?>> getDependedOn() {
        return Optional.empty();
    }

    @Override
    protected Supplier<Optional<String>> getLatestSource() {
        return NotificationProperties::getPropertiesContents;
    }

    @Override
    protected Optional<NotificationProperties> transform(final String source) {
        return NotificationProperties.getProperties(source);
    }

}
