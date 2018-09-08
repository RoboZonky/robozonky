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

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.function.Consumer;

import com.github.robozonky.common.jobs.Job;
import com.github.robozonky.common.jobs.Payload;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.internal.api.Defaults;

final class StonkyJob implements Job {

    private final Random random = new Random();

    private final Consumer<SecretProvider> stonky;

    public StonkyJob() {
        this(arg -> new Stonky().apply(arg));
    }

    StonkyJob(final Consumer<SecretProvider> provider) {
        this.stonky = provider;
    }

    @Override
    public Duration startIn() {
        final int randomSeconds = random.nextInt(1000);
        final ZonedDateTime triggerOn =
                LocalDate.now().plusDays(1).atStartOfDay(Defaults.ZONE_ID).plusSeconds(randomSeconds);
        return Duration.between(ZonedDateTime.now(), triggerOn);
    }

    @Override
    public Duration repeatEvery() {
        return Duration.ofHours(24);
    }

    @Override
    public Duration killIn() {
        return Duration.ofMinutes(30);
    }

    @Override
    public Payload payload() {
        return new StonkyPayload();
    }

    private final class StonkyPayload implements Payload {

        @Override
        public String id() {
            return "Stonky";
        }

        @Override
        public void accept(final SecretProvider secretProvider) {
            stonky.accept(secretProvider);
        }
    }
}
