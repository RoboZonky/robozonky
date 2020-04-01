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

package com.github.robozonky.internal;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SettingsTest {

    @AfterEach
    void reinit() {
        Settings.INSTANCE.reinit();
        System.clearProperty(Settings.FILE_LOCATION_PROPERTY);
    }

    @Test
    void defaultProperties() {
        assertSoftly(softly -> {
            softly.assertThat(Settings.INSTANCE.get("user.dir", ""))
                .isNotEqualTo("");
            softly.assertThat(Settings.INSTANCE.get(UUID.randomUUID()
                .toString(), ""))
                .isEqualTo("");
            softly.assertThat(Settings.INSTANCE.getRemoteResourceRefreshInterval())
                .matches(new SettingsTest.TemporalPredicate(5 * 60));
            softly.assertThat(Settings.INSTANCE.getDryRunBalanceMinimum())
                .isEqualTo(-1);
            softly.assertThat(Settings.INSTANCE.getMaxItemsReadFromPrimaryMarketplace())
                .isEqualTo(-1);
            softly.assertThat(Settings.INSTANCE.getMaxItemsReadFromSecondaryMarketplace())
                .isEqualTo(500);
            softly.assertThat(Settings.INSTANCE.getSocketTimeout())
                .matches(new SettingsTest.TemporalPredicate(10));
            softly.assertThat(Settings.INSTANCE.getConnectionTimeout())
                .matches(new SettingsTest.TemporalPredicate(10));
            softly.assertThat(Settings.INSTANCE.getDefaultApiPageSize())
                .isEqualTo(100);
            softly.assertThat(Settings.INSTANCE.getHttpsProxyPort())
                .isEqualTo(443);
            softly.assertThat(Settings.INSTANCE.getHttpsProxyHostname())
                .isEmpty();
            softly.assertThat(Settings.INSTANCE.isDebugHttpResponseLoggingEnabled())
                .isFalse();
        });
    }

    @Test
    void setProperties() throws IOException {
        final Properties p = new Properties();
        Stream.of(Settings.Key.values())
            .forEach(v -> p.setProperty(v.getName(), "2000"));
        p.setProperty(Settings.Key.DEBUG_ENABLE_HTTP_RESPONSE_LOGGING.getName(), "true");
        final File f = File.createTempFile("robozonky-", ".properties");
        p.store(new FileWriter(f), "Testing properties");
        System.setProperty(Settings.FILE_LOCATION_PROPERTY, f.getAbsolutePath());
        assertSoftly(softly -> {
            softly.assertThat(Settings.INSTANCE.get("user.dir", ""))
                .isNotEqualTo("");
            softly.assertThat(Settings.INSTANCE.get(UUID.randomUUID()
                .toString(), ""))
                .isEqualTo("");
            softly.assertThat(Settings.INSTANCE.isDebugHttpResponseLoggingEnabled())
                .isTrue();
            softly.assertThat(Settings.INSTANCE.getRemoteResourceRefreshInterval())
                .matches(new SettingsTest.TemporalPredicate(2000 * 60));
            softly.assertThat(Settings.INSTANCE.getDryRunBalanceMinimum())
                .isEqualTo(2000);
            softly.assertThat(Settings.INSTANCE.getMaxItemsReadFromPrimaryMarketplace())
                .isEqualTo(2000);
            softly.assertThat(Settings.INSTANCE.getMaxItemsReadFromSecondaryMarketplace())
                .isEqualTo(2000);
            softly.assertThat(Settings.INSTANCE.getSocketTimeout())
                .matches(new SettingsTest.TemporalPredicate(2000));
            softly.assertThat(Settings.INSTANCE.getConnectionTimeout())
                .matches(new SettingsTest.TemporalPredicate(2000));
            softly.assertThat(Settings.INSTANCE.getDefaultApiPageSize())
                .isEqualTo(2000);
            softly.assertThat(Settings.INSTANCE.getHttpsProxyPort())
                .isEqualTo(2000);
            softly.assertThat(Settings.INSTANCE.getHttpsProxyHostname())
                .contains("2000");
        });
    }

    private static final class TemporalPredicate implements Predicate<TemporalAmount> {

        private final long seconds;

        TemporalPredicate(final long seconds) {
            this.seconds = seconds;
        }

        @Override
        public boolean test(final TemporalAmount o) {
            return o.get(ChronoUnit.SECONDS) == seconds;
        }

        @Override
        public String toString() {
            return "seconds = " + seconds;
        }
    }
}
