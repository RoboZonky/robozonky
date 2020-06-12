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

package com.github.robozonky.cli.configuration.scripts;

import java.io.File;
import java.util.List;

final class UnixRunScriptGenerator extends RunScriptGenerator {

    private static final String EXEC_NAME = "robozonky-exec.sh";

    public UnixRunScriptGenerator(final File distributionDirectory, final File configFile) {
        super(distributionDirectory, configFile);
    }

    @Override
    public File apply(final List<String> javaOpts) {
        return process(javaOpts, EXEC_NAME + ".ftl");
    }

    @Override
    protected File getRunScript() {
        return new File(this.getRootFolder(), EXEC_NAME);
    }

    @Override
    public File getChildRunScript() {
        return new File(distributionDirectory, "robozonky.sh");
    }
}
