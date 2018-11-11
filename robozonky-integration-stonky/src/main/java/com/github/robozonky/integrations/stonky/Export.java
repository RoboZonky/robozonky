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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.ws.rs.core.Response;

import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.ZonkyScope;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.util.Backoff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum Export {

    WALLET(Zonky::requestWalletExport, Zonky::downloadWalletExport),
    INVESTMENTS(Zonky::requestInvestmentsExport, Zonky::downloadInvestmentsExport);

    private static final Logger LOGGER = LoggerFactory.getLogger(Export.class);
    private final Consumer<Zonky> trigger;
    private final Function<Zonky, URL> download;

    Export(final Consumer<Zonky> trigger, final Function<Zonky, Response> delegate) {
        this.trigger = trigger;
        this.download = api -> download(api, delegate);
    }

    private static URL download(final Zonky zonky, final Function<Zonky, Response> delegate) {
        final Response response = delegate.apply(zonky);
        try {
            final int status = response.getStatus();
            LOGGER.debug("Download endpoint returned HTTP {}.", status);
            if (status == 302) {
                try {
                    final String s = response.getHeaderString("Location");
                    return new URL(s);
                } catch (final MalformedURLException ex) {
                    LOGGER.warn("Proper HTTP response, improper redirect location.", ex);
                }
            }
            return null;
        } finally { // not using try-with-resources, as that'd generate several untestable PITest mutations
            response.close();
        }
    }

    public CompletableFuture<Optional<File>> download(final Tenant tenant) {
        final Backoff<URL> waitWhileExportRunning =
                Backoff.exponential(() -> tenant.call(download, ZonkyScope.FILES), Duration.ofSeconds(1),
                                    Duration.ofHours(1));
        return CompletableFuture.runAsync(() -> tenant.run(trigger, ZonkyScope.APP))
                .thenApplyAsync(v -> waitWhileExportRunning.get())
                .thenApplyAsync(url -> url.flatMap(Util::download));
    }
}
