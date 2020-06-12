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

package com.github.robozonky.cli.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

final class LocalStrategy implements StrategyConfiguration {

    private final String strategyLocation;
    private final AtomicReference<String> finalLocation = new AtomicReference<>();

    public LocalStrategy(final String strategyLocation) {
        this.strategyLocation = strategyLocation;
    }

    @Override
    public String getFinalLocation() {
        return finalLocation.get();
    }

    @Override
    public void accept(Path distributionRoot, Path installationRoot) {
        var sourcePath = Path.of(strategyLocation)
            .toAbsolutePath();
        if (!sourcePath.toFile()
            .canRead()) {
            throw new IllegalStateException("Cannot read strategy: " + strategyLocation);
        }
        var target = installationRoot.resolve("robozonky-strategy.cfg")
            .toAbsolutePath();
        try {
            Util.copy(sourcePath, target);
            finalLocation.set(target.toString());
        } catch (IOException ex) {
            throw new IllegalStateException("Can not copy strategy: " + strategyLocation, ex);
        }
    }
}
