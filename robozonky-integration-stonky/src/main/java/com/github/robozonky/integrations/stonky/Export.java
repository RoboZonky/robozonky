/*
 * Copyright 2019 The RoboZonky Project
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
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.ws.rs.core.Response;

import com.github.robozonky.api.remote.enums.OAuthScope;
import com.github.robozonky.common.async.Backoff;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

enum Export {

    WALLET(Zonky::requestWalletExport, Zonky::downloadWalletExport),
    INVESTMENTS(Zonky::requestInvestmentsExport, Zonky::downloadInvestmentsExport);

    private static final Logger LOGGER = LogManager.getLogger(Export.class);
    private final Consumer<Zonky> trigger;
    private final Function<Zonky, URL> download;

    Export(final Consumer<Zonky> trigger, final Function<Zonky, Response> delegate) {
        this.trigger = trigger;
        this.download = api -> download(api, delegate);
    }

    private URL download(final Zonky zonky, final Function<Zonky, Response> delegate) {
        return Try.withResources(() -> delegate.apply(zonky))
                .of(response -> {
                    final int status = response.getStatus();
                    LOGGER.debug("Download endpoint returned HTTP {}.", status);
                    if (status != 302) {
                        throw new IllegalStateException("Download not yet ready: " + this);
                    }
                    final String s = response.getHeaderString("Location");
                    return new URL(s);
                }).get();
    }

    public CompletableFuture<Optional<File>> download(final Tenant tenant, final Duration backoffTime) {
        final Backoff<URL> waitWhileExportRunning = Backoff.exponential(() -> tenant.call(download, OAuthScope.SCOPE_FILE_DOWNLOAD),
                                                                        Duration.ofSeconds(1), backoffTime);
        return CompletableFuture.runAsync(() -> tenant.run(trigger, OAuthScope.SCOPE_APP_WEB))
                .thenApplyAsync(v -> waitWhileExportRunning.get())
                .thenApplyAsync(urlOrError -> urlOrError.fold(r -> Optional.empty(), Util::download));
    }

    public CompletableFuture<Optional<File>> download(final Tenant tenant) {
        return download(tenant, Duration.ofHours(1));
    }
}
