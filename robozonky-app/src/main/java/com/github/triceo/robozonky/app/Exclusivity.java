/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to ensure that only one RoboZonky process may be executing at any one time. Call {@link #ensure()} at the
 * start of RoboZonky to make it happen. Will not mutually exclude RoboZonky instances running within the same JVM.
 */
enum Exclusivity {

    /**
     * Simple cheap thread-safe singleton.
     */
    INSTANCE;

    static final File ROBOZONKY_LOCK = new File(System.getProperty("java.io.tmpdir"), "robozonky.lock");
    private static final Logger LOGGER = LoggerFactory.getLogger(Exclusivity.class);

    private FileLock lock = null;

    /**
     * Whether or not the application has managed to acquire exclusivity.
     * @return  True between any two successful successive calls to {@link #ensure()} and {@link #waive()} respectively.
     * Will be false before first successful {@link #ensure()} or after every {@link #waive()}.
     */
    public synchronized boolean isEnsured() {
        return lock != null && lock.isValid() && !lock.isShared();
    }

    /**
     * Allow other instances of RoboZonky to run, possibly acquiring exclusivity for themselves.
     * @throws IOException If releasing the lock failed. Not much to do there.
     */
    public synchronized void waive() throws IOException {
        if (!this.isEnsured()) {
            Exclusivity.LOGGER.debug("Already waived.");
            return;
        }
        final FileLock currentLock = this.lock;
        try {
            currentLock.release();
            this.lock = null;
            Exclusivity.LOGGER.debug("File lock released.");
        } finally {
            try (final Channel channel = currentLock.acquiredBy()) {
                if (channel.isOpen()) {
                    channel.close();
                }
            } catch (final IOException ex) {
                Exclusivity.LOGGER.debug("Failed closing lock file channel.", ex);
            } finally {
                if (!Exclusivity.ROBOZONKY_LOCK.delete()) {
                    Exclusivity.LOGGER.debug("Failed deleting lock file.");
                }
            }
        }
    }

    /**
     * Will return after this instance of RoboZonky has acquired exclusivity. May block for extensive periods of time.
     * @throws IOException When locking failed.
     */
    public synchronized void ensure() throws IOException {
        if (this.isEnsured()) {
            Exclusivity.LOGGER.debug("Already ensured.");
            return;
        }
        Exclusivity.LOGGER.info("Checking we're the only RoboZonky running.");
        Exclusivity.LOGGER.debug("Acquiring file lock: {}.", Exclusivity.ROBOZONKY_LOCK.getAbsolutePath());
        final FileChannel ch = new RandomAccessFile(Exclusivity.ROBOZONKY_LOCK, "rw").getChannel();
        this.lock = ch.lock();
        Exclusivity.LOGGER.debug("File lock acquired.");
    }

}
