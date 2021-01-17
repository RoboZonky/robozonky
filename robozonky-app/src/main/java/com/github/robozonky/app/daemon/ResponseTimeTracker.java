/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;

/**
 * Used to debug how long it takes for a loan to be invested into, or a participation to be purchased.
 * The times are in nanosecond from the moment the robot was first notified of the item, to the moment that the robot
 * triggered the API request to invest or purchase.
 */
final class ResponseTimeTracker {

    private static final Logger LOGGER = LogManager.getLogger(ResponseTimeTracker.class);
    private static final ResponseTimeTracker INSTANCE = new ResponseTimeTracker();

    private final Map<Long, Long> loanRegistrations = new ConcurrentHashMap<>(0);
    private final Map<Long, Long> participationRegistrations = new ConcurrentHashMap<>(0);
    private final File loanOutputFile = new File("debug-loans.txt");
    private final File participationOutputFile = new File("debug-participations.txt");
    private final Map<File, List<String>> toWrite = new ConcurrentHashMap<>(2);

    private ResponseTimeTracker() {
        // No external instances.
    }

    private static void write(File file, String contents) {
        try (var writer = Files.newBufferedWriter(file.toPath(), WRITE, APPEND)) {
            writer.write(contents);
            writer.newLine();
        } catch (IOException ex) {
            LOGGER.trace("Failed writing '{}' to {}.", contents, file);
        }
    }

    public static void executeAsync(BiConsumer<ResponseTimeTracker, Long> operation) {
        var nanotime = System.nanoTime(); // Store current nanotime, as we can't control when the operation will run.
        CompletableFuture.runAsync(() -> operation.accept(INSTANCE, nanotime));
    }

    /**
     * Register that a {@link Loan} entered the system at this time.
     * 
     * @param nanotime
     * @param id
     */
    public void registerLoan(long nanotime, long id) {
        loanRegistrations.putIfAbsent(id, nanotime);
    }

    /**
     * Register that a {@link Participation} entered the system at this time.
     * 
     * @param nanotime
     * @param id
     */
    public void registerParticipation(long nanotime, long id) {
        participationRegistrations.putIfAbsent(id, nanotime);
    }

    /**
     * Register that an investment attempt was made.
     * 
     * @param nanotime
     * @param loan
     */
    public void dispatch(long nanotime, Loan loan) {
        dispatch(nanotime, (long) loan.getId(), loanRegistrations, loanOutputFile);
    }

    /**
     * Register that a purchase attempt was made.
     * 
     * @param nanotime
     * @param participation
     */
    public void dispatch(long nanotime, Participation participation) {
        dispatch(nanotime, participation.getId(), participationRegistrations, participationOutputFile);
    }

    private <Id extends Number> void dispatch(long nanotime, Id id, Map<Id, Long> registrations, File output) {
        var registeredOn = registrations.remove(id);
        if (registeredOn == null) {
            LOGGER.trace("No registration found for #{}.", id);
            return;
        }
        var nanosDuration = nanotime - registeredOn;
        var content = id + " " + nanosDuration;
        toWrite.compute(output, (f, contents) -> {
            var strings = contents == null ? new CopyOnWriteArrayList<String>() : contents;
            strings.add(content);
            return strings;
        });
    }

    private void clear(File file, Map<Long, Long> registrations) {
        try {
            var contents = toWrite.remove(file);
            if (contents == null) {
                return;
            }
            write(file, contents.stream()
                .collect(joining(System.lineSeparator())));
        } catch (Exception ex) {
            LOGGER.trace("Failed writing into {}.", file, ex);
        } finally {
            registrations.clear();
        }
    }

    /**
     * To be called at the end of operations to write the results and clear any undispatched items.
     */
    public void clear() {
        synchronized (loanOutputFile) {
            clear(loanOutputFile, loanRegistrations);
        }
        synchronized (participationOutputFile) {
            clear(participationOutputFile, participationRegistrations);
        }
    }

}
