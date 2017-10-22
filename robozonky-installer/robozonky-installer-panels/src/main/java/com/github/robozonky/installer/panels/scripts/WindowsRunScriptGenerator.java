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

package com.github.robozonky.installer.panels.scripts;

import java.io.File;
import java.util.regex.Pattern;

import com.github.robozonky.installer.panels.CommandLinePart;

final class WindowsRunScriptGenerator extends RunScriptGenerator {

    private static final Pattern UNIX_NEWLINES = Pattern.compile("(\n)+");

    public WindowsRunScriptGenerator(final File configFile) {
        super(configFile);
    }

    @Override
    public File apply(final CommandLinePart commandLine) {
        return process(commandLine, "windows.ftl", s -> UNIX_NEWLINES.matcher(s).replaceAll("\r\n"));
    }

    @Override
    protected File getRunScript(final File parentFolder) {
        return new File(parentFolder, "run.bat");
    }
}
