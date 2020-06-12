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

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ServiceGeneratorTest {

    @Test
    void systemd() throws IOException {
        ServiceGenerator generator = ServiceGenerator.SYSTEMD;
        Path path = Files.createTempFile("robozonky-", ".service");
        File result = generator.apply(path.toFile());
        String content = Files.readString(result.toPath());
        assertThat(content).isEqualTo("[Unit]\n" +
                "Description=RoboZonky: Automated Zonky.cz investing robot\n" +
                "After=network.target\n" +
                "\n" +
                "[Service]\n" +
                "User=robozonky\n" +
                "Group=robozonky\n" +
                "Restart=always\n" +
                "WorkingDirectory=" + path.getParent() + "\n" +
                "ExecStart=" + path + "\n" +
                "ExecStop=\n" +
                "\n" +
                "[Install]\n" +
                "WantedBy=multi-user.target");
    }

}
