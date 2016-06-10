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

package com.github.triceo.robozonky.app.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple abstraction for dealing with the overly complicated {@link KeyStore} API. Always call {@link #save()} to
 * persist changes made.
 */
public class KeyStoreHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreHandler.class);
    private static final String KEYSTORE_TYPE = "JCEKS";
    private static final String KEY_TYPE = "PBE";

    /**
     * Create brand new key store protected by a given password.
     *
     * @param keyStoreFile The file where the key store should be.
     * @param password Password to protect the key store.
     * @return
     * @throws IOException If file already exists or there is a problem writing the file.
     * @throws KeyStoreException If something's happened to the key store.
     */
    public static KeyStoreHandler create(final File keyStoreFile, final String password)
            throws IOException, KeyStoreException {
        if (keyStoreFile.exists()) {
            throw new FileAlreadyExistsException(keyStoreFile.getAbsolutePath());
        }
        final KeyStore ks = KeyStore.getInstance(KeyStoreHandler.KEYSTORE_TYPE);
        // get user password and file input stream
        final char[] passwordArray = password.toCharArray();
        try {
            final SecretKeyFactory factory = SecretKeyFactory.getInstance(KeyStoreHandler.KEY_TYPE);
            ks.load(null, passwordArray);
            return new KeyStoreHandler(ks, passwordArray, keyStoreFile, factory);
        } catch (final NoSuchAlgorithmException | CertificateException ex) {
            throw new IllegalStateException("Should not happen.", ex);
        }
    }

    /**
     * Open an existing key store, protected by a given password.
     *
     * @param keyStoreFile The file where the key store is.
     * @param password Password that protects the key store.
     * @return
     * @throws IOException If file does not exist or there is a problem writing the file.
     * @throws KeyStoreException If something's happened to the key store.
     */
    public static KeyStoreHandler open(final File keyStoreFile, final String password)
            throws IOException, KeyStoreException {
        if (!keyStoreFile.exists()) {
            throw new FileNotFoundException(keyStoreFile.getAbsolutePath());
        }
        final KeyStore ks = KeyStore.getInstance(KeyStoreHandler.KEYSTORE_TYPE);
        // get user password and file input stream
        final char[] passwordArray = password.toCharArray();
        try (FileInputStream fis = new FileInputStream(keyStoreFile)) {
            final SecretKeyFactory factory = SecretKeyFactory.getInstance(KeyStoreHandler.KEY_TYPE);
            ks.load(fis, passwordArray);
            return new KeyStoreHandler(ks, passwordArray, keyStoreFile, factory);
        } catch (final NoSuchAlgorithmException | CertificateException ex) {
            throw new IllegalStateException("Should not happen.", ex);
        }
    }

    private final char[] password;
    private final File keyStoreFile;
    private final KeyStore keyStore;
    private final KeyStore.ProtectionParameter protectionParameter;
    private final SecretKeyFactory keyFactory;

    private KeyStoreHandler(final KeyStore keyStore, final char[] password, final File keyStoreFile,
                            final SecretKeyFactory keyFactory) {
        this.keyStore = keyStore;
        this.password = password;
        this.protectionParameter = new KeyStore.PasswordProtection("NO_PASSWORD".toCharArray()); // FIXME is this safe?
        this.keyStoreFile = keyStoreFile;
        this.keyFactory = keyFactory;
    }

    /**
     * Set a key in the key store. Uses {@link SecretKey} as the implementation. Overwrites previous contents, if any.
     *
     * @param alias Alias to store the key under.
     * @param valueStream Will be converted to a {@link String} and stored.
     * @return True if success.
     * @throws IOException If stream failed to read.
     */
    public boolean set(final String alias, final InputStream valueStream) throws IOException {
        return this.set(alias, IOUtils.toString(valueStream, "UTF-8"));
    }

    /**
     * Set a key in the key store. Uses {@link SecretKey} as the implementation. Overwrites previous contents, if any.
     *
     * @param alias Alias to store the key under.
     * @param value The value to be stored.
     * @return True if stored in the key store.
     */
    public boolean set(final String alias, final String value) {
        try {
            final SecretKey secret = this.keyFactory.generateSecret(new PBEKeySpec(value.toCharArray()));
            final KeyStore.Entry skEntry = new KeyStore.SecretKeyEntry(secret);
            this.keyStore.setEntry(alias, skEntry, this.protectionParameter);
            return true;
        } catch (final KeyStoreException | InvalidKeySpecException ex) {
            KeyStoreHandler.LOGGER.debug("Failed storing '{}'.", alias, ex);
            return false;
        }
    }

    /**
     * Retrieve a previously stored key.
     *
     * @param alias The alias under which the key will be looked up.
     * @return Present if the alias is present in the key store.
     */
    public Optional<String> get(final String alias) {
        try {
            final KeyStore.SecretKeyEntry skEntry =
                    (KeyStore.SecretKeyEntry)this.keyStore.getEntry(alias, this.protectionParameter);
            if (skEntry == null) {
                return Optional.empty();
            }
            final PBEKeySpec keySpec = (PBEKeySpec)this.keyFactory.getKeySpec(skEntry.getSecretKey(), PBEKeySpec.class);
            return Optional.of(new String(keySpec.getPassword()));
        } catch (final NoSuchAlgorithmException | KeyStoreException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Should not happen.", ex);
        } catch (final UnrecoverableEntryException ex) {
            KeyStoreHandler.LOGGER.debug("Unrecoverable entry '{}'.", alias, ex);
            return Optional.empty();
        }
    }

    /**
     * Remove an entry from the key store.
     *
     * @param alias The alias to locate the entry.
     * @return True if there is now no entry with a given key.
     */
    public boolean delete(final String alias) {
        try {
            this.keyStore.deleteEntry(alias);
            return true;
        } catch (final KeyStoreException ex) {
            KeyStoreHandler.LOGGER.debug("Entry '{}' not deleted.", alias, ex);
            return false;
        }
    }

    /**
     * Persist whatever operations that have been made using this API. Unless this method is called, no other methods
     * have effect.
     *
     * @throws IOException If saving the key store failed.
     */
    public void save() throws IOException {
        try (final OutputStream os = new BufferedOutputStream(new FileOutputStream(this.keyStoreFile))) {
            try {
                this.keyStore.store(os, this.password);
            } catch (final KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
                throw new IllegalStateException("Should not happen.", ex);
            }
        }
    }

}
