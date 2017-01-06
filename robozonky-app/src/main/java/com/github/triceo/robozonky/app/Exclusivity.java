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

package com.github.triceo.robozonky.app;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.ReturnCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to ensure that only one RoboZonky process may be executing at any one time. See {@link #get()} for the
 * contract. Will not mutually exclude RoboZonky instances running within the same JVM.
 */
final class Exclusivity implements ShutdownHook.Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Exclusivity.class);

    private static final class LockControl {

        private final RandomAccessFile randomAccessFile;
        private final FileLock lock;

        public LockControl(final File lockedFile) throws IOException {
            this.randomAccessFile = new RandomAccessFile(lockedFile, "rw");
            this.lock = this.randomAccessFile.getChannel().lock();
        }

        public boolean hasValidLock() {
            return lock.isValid();
        }

        public void invalidateLock() throws IOException {
            this.randomAccessFile.close();
        }

    }

    private final File fileToLock;
    private Exclusivity.LockControl lockControl = null;

    /**
     * Guarantees exclusivity based on a randomly created temp file. This is only useful for testing, since in all
     * other cases multiple JVMs need to synchronize around the same file.
     * @throws IOException When the temp file could not be created.
     */
    Exclusivity() throws IOException {
        this(File.createTempFile("robozonky-", ".lock"));
    }

    /**
     * Guarantees exclusivity around a provided file.
     * @param lock File to lock on.
     */
    public Exclusivity(final File lock) {
        this.fileToLock = lock;
    }

    File getFileToLock() {
        return this.fileToLock;
    }

    /**
     * Whether or not the application has managed to acquire exclusivity.
     * @return  True between any two successful successive calls to {@link #ensure()} and {@link #waive()} respectively.
     * Will be false before first successful {@link #ensure()} or after every {@link #waive()}.
     */
    synchronized boolean isEnsured() {
        return lockControl != null && lockControl.hasValidLock();
    }

    /**
     * Allow other instances of RoboZonky to run, possibly acquiring exclusivity for themselves.
     */
    synchronized void waive() {
        if (!this.isEnsured()) {
            Exclusivity.LOGGER.debug("Already waived.");
            return;
        }
        try {
            this.lockControl.invalidateLock();
            Exclusivity.LOGGER.debug("File lock released.");
        } catch (final IOException ex) {
            Exclusivity.LOGGER.warn("Failed releasing lock, new RoboZonky processes may not launch.", ex);
        } finally {
            Exclusivity.LOGGER.debug("Lock file deleted successfully: {}.", this.fileToLock.delete());
        }
    }

    /**
     * Will return after this instance of RoboZonky has acquired exclusivity, or failed. May block for extensive periods
     * of time. Make sure to call {@link #isEnsured()} afterwards to see if exclusivity is ensured.
     */
    synchronized void ensure() {
        if (this.isEnsured()) {
            Exclusivity.LOGGER.debug("Already ensured.");
            return;
        }
        Exclusivity.LOGGER.info("Checking we're the only RoboZonky running.");
        Exclusivity.LOGGER.debug("Acquiring file lock: {}.", this.fileToLock.getAbsolutePath());
        try {
            this.lockControl = new Exclusivity.LockControl(this.fileToLock);
            Exclusivity.LOGGER.debug("File lock acquired.");
        } catch (final IOException ex) {
            Exclusivity.LOGGER.error("Failed acquiring lock, another RoboZonky process likely running.", ex);
        }
    }

    /**
     * Call in order to acquire exclusivity. May block for extensive periods of time until exclusivity is acquired.
     * @return If present, call in order to waive exclusivity. If empty, exclusivity not acquired.
     */
    @Override
    public Optional<Consumer<ReturnCode>> get() {
        this.ensure();
        if (!this.isEnsured()) {
            return Optional.empty();
        } else { // other RoboZonky instances can now start executing
            return Optional.of((code) -> this.waive());
        }
    }
}
