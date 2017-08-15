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

package com.github.triceo.robozonky.strategy.natural;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.github.triceo.robozonky.internal.api.Defaults;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class MainTest {

    @Test
    public void noArgument() {
        Assertions.assertThatThrownBy(Main::main).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void moreArguments() {
        Assertions.assertThatThrownBy(() -> Main.main("a", "b")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void nonexistentFile() {
        Assertions.assertThatThrownBy(() -> Main.main(UUID.randomUUID().toString()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void proper() throws IOException {
        // prepare a file with strategy
        final File tmp = File.createTempFile("robozonky-", ".tmp");
        final String strategy = IOUtils.toString(getClass().getResourceAsStream("complex"), Defaults.CHARSET);
        IOUtils.write(strategy, new FileOutputStream(tmp), Defaults.CHARSET);
        // read the file, not failing if all is OK
        Main.main(tmp.getAbsolutePath());
    }
}
