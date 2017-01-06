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

package com.github.triceo.robozonky.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.StringJoiner;

public class IoTestUtil {

    public static File streamToFile(final InputStream s) throws IOException {
        return IoTestUtil.streamToFile(s, ".tmp");
    }

    public static File streamToFile(final InputStream fis, final String extension) throws IOException {
        final File f = File.createTempFile("robozonky-", extension);
        // binary copy since the stream is not guaranteed textual
        try (final OutputStream fos = Files.newOutputStream(f.toPath())) {
            final byte[] buffer = new byte[1024];
            int noOfBytes = 0;
            while ((noOfBytes = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, noOfBytes);
            }
        }
        return f;
    }

    private static final String ROOT = new StringJoiner(File.separator).add("src").add("main").toString();

    public static String findMainSource() {
        final File current = new File(System.getProperty("user.dir"));
        final File f = new File(current, IoTestUtil.ROOT);
        if (f.exists()) {
            return f.getAbsolutePath();
        }
        final File f2 = new File(current.getParent(), IoTestUtil.ROOT); // in case we're running from /target
        if (f2.exists()) {
            return f2.getAbsolutePath();
        }
        throw new IllegalStateException("Oops.");
    }

    public static String findMainSource(String... subfolders) {
        final StringJoiner sj = new StringJoiner(File.separator);
        sj.add(IoTestUtil.findMainSource());
        Arrays.stream(subfolders).forEach(sj::add);
        return sj.toString();
    }

}
