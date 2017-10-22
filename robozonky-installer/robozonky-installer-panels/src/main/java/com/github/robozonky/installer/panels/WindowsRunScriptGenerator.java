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

package com.github.robozonky.installer.panels;

import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;

final class WindowsRunScriptGenerator extends AbstractRunScriptGenerator {

    public WindowsRunScriptGenerator(final File distributionFolder) {
        super(distributionFolder);
    }

    @Override
    public String apply(final CommandLinePart commandLine) {
        final Collection<String> result = this.getCommonScript(commandLine, (s, s2) -> "set \"" + s + "=" + s2 + "\"",
                                                               "set \"JAVA_OPTS=%JAVA_OPTS% ", "robozonky.bat");
        return result.stream().collect(Collectors.joining("\r\n"));
    }

    @Override
    public File getRunScript(final File parentFolder) {
        return new File(parentFolder, "run.bat");
    }
}
