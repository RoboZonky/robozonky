/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.common.secrets;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple abstraction for dealing with the overly complicated {@link KeyStore} API. Always call {@link #save()} to
 * persist changes made.
 */
public class KeyStoreHandler {

    private static final Logger LOGGER = LogManager.getLogger(KeyStoreHandler.class);
    private static final String KEYSTORE_TYPE = "JCEKS";
    private static final String KEY_TYPE = "PBE";
    private final AtomicBoolean dirty;
    private final File keyStoreFile;
    private final KeyStore keyStore;
    private final KeyStore.ProtectionParameter protectionParameter;
    private final SecretKeyFactory keyFactory;
    private char[] password;

    /**
     * Create a new instance, where {@link #isDirty()} will be false.
     * @param keyStore KeyStore to use as the backend.
     * @param password Password to protect the keys.
     * @param keyStoreFile File that will represent the keystore.
     * @param keyFactory Factory to create the keys.
     */
    private KeyStoreHandler(final KeyStore keyStore, final char[] password, final File keyStoreFile,
                            final SecretKeyFactory keyFactory) {
        this(keyStore, password, keyStoreFile, keyFactory, true);
    }

    private KeyStoreHandler(final KeyStore keyStore, final char[] password, final File keyStoreFile,
                            final SecretKeyFactory keyFactory, final boolean isDirty) {
        this.keyStore = keyStore;
        this.password = password.clone();
        this.protectionParameter = new KeyStore.PasswordProtection("NO_PASSWORD".toCharArray()); // FIXME is this safe?
        this.keyStoreFile = keyStoreFile;
        this.keyFactory = keyFactory;
        this.dirty = new AtomicBoolean(isDirty);
    }

    private static SecretKeyFactory getSecretKeyFactory() {
        try {
            return SecretKeyFactory.getInstance(KEY_TYPE);
        } catch (final Exception ex) { // otherwise we're seing security-related flakiness on Windows-based CI
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Create brand new key store protected by a given password, and store it in a file.
     * @param keyStoreFile The file where the key store should be.
     * @param password Password to protect the key store.
     * @return Freshly instantiated key store, in a newly created file.
     * @throws IOException If file already exists or there is a problem writing the file.
     * @throws KeyStoreException If something's happened to the key store.
     */
    public static KeyStoreHandler create(final File keyStoreFile, final char... password)
            throws IOException, KeyStoreException {
        if (keyStoreFile == null) {
            throw new FileNotFoundException(null);
        } else if (keyStoreFile.exists()) {
            throw new FileAlreadyExistsException(keyStoreFile.getAbsolutePath());
        }
        final KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
        // get user password and file input stream
        try {
            ks.load(null, password);
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }
        // store the newly created key store
        final SecretKeyFactory skf = getSecretKeyFactory();
        final KeyStoreHandler ksh = new KeyStoreHandler(ks, password, keyStoreFile, skf);
        ksh.save();
        return ksh;
    }

    /**
     * Open an existing key store, protected by a given password.
     * @param keyStoreFile The file where the key store is.
     * @param password Password that protects the key store.
     * @return Key store that previously existed.
     * @throws IOException If file does not exist or there is a problem writing the file.
     * @throws KeyStoreException If something's happened to the key store.
     */
    public static KeyStoreHandler open(final File keyStoreFile,
                                       final char... password) throws IOException, KeyStoreException {
        if (keyStoreFile == null) {
            throw new FileNotFoundException(null);
        } else if (!keyStoreFile.exists()) {
            throw new FileNotFoundException(keyStoreFile.getAbsolutePath());
        }
        final KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
        // get user password and file input stream
        return Try.withResources(() -> new FileInputStream(keyStoreFile))
                .of(fis -> {
                    ks.load(fis, password);
                    return new KeyStoreHandler(ks, password, keyStoreFile, getSecretKeyFactory(),
                                               false);
                })
                .getOrElseThrow((Function<Throwable, IllegalStateException>) IllegalStateException::new);
    }

    /**
     * Set a key in the key store. Uses {@link SecretKey} as the implementation. Overwrites previous contents, if any.
     * @param alias Alias to store the key under.
     * @param value The value to be stored.
     * @return True if stored in the key store.
     */
    public boolean set(final String alias, final char[] value) {
        return Try.of(() -> {
            final SecretKey secret = this.keyFactory.generateSecret(new PBEKeySpec(value));
            final KeyStore.Entry skEntry = new KeyStore.SecretKeyEntry(secret);
            this.keyStore.setEntry(alias, skEntry, this.protectionParameter);
            this.dirty.set(true);
            return true;
        }).getOrElseGet(t -> {
            LOGGER.debug("Failed storing '{}'.", alias, t);
            return false;
        });
    }

    /**
     * Retrieve a previously stored key.
     * @param alias The alias under which the key will be looked up.
     * @return Present if the alias is present in the key store.
     */
    public Optional<char[]> get(final String alias) {
        return Try.of(() -> {
            final KeyStore.SecretKeyEntry skEntry =
                    (KeyStore.SecretKeyEntry) this.keyStore.getEntry(alias, this.protectionParameter);
            if (skEntry == null) {
                return Optional.<char[]>empty();
            }
            final PBEKeySpec keySpec = (PBEKeySpec) this.keyFactory.getKeySpec(skEntry.getSecretKey(),
                                                                               PBEKeySpec.class);
            return Optional.of(keySpec.getPassword());
        }).getOrElseGet(t -> {
            LOGGER.debug("Unrecoverable entry '{}'.", alias, t);
            return Optional.empty();
        });
    }

    /**
     * Remove an entry from the key store.
     * @param alias The alias to locate the entry.
     * @return True if there is now no entry with a given key.
     */
    public boolean delete(final String alias) {
        return Try.of(() -> {
            this.keyStore.deleteEntry(alias);
            this.dirty.set(true);
            return true;
        }).getOrElseGet(t -> {
            LOGGER.debug("Entry '{}' not deleted.", alias, t);
            return false;
        });
    }

    /**
     * Whether or not there are unsaved changes.
     * @return Whether a {@link #set(String, char[])} occurred after last {@link #save()}.
     */
    public boolean isDirty() {
        return this.dirty.get();
    }

    /**
     * Persist whatever operations that have been made using this API. Unless this method is called, no other methods
     * have effect.
     */
    public void save() {
        save(this.password);
    }

    /**
     * Persist whatever operations that have been made using this API. Unless this method is called, no other methods
     * have effect.
     * @param secret Password to persist the changes with.
     */
    public void save(final char... secret) {
        this.password = secret.clone();
        Try.withResources(() -> new BufferedOutputStream(new FileOutputStream(this.keyStoreFile)))
                .of(os -> {
                    this.keyStore.store(os, secret);
                    this.dirty.set(false);
                    return null;
                })
                .getOrElseThrow((Function<Throwable, IllegalStateException>) IllegalStateException::new);
    }
}
