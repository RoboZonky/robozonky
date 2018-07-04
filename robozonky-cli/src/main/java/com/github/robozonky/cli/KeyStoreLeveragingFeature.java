/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.cli;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.github.robozonky.common.secrets.KeyStoreHandler;

abstract class KeyStoreLeveragingFeature implements Feature {

    @Parameter(order = 0, names = {"-k", "--keystore"}, description = "The keystore to hold the secrets.",
            required = true)
    private File keystore = new File("robozonky.keystore");
    @Parameter(order = 1, names = {"-s", "--secret"}, description = "Secret to use to access the keystore.",
            converter = PasswordConverter.class, required = true, password = true)
    private char[] secret = null;
    private KeyStoreHandler storage;

    protected KeyStoreLeveragingFeature(final File keystore, final char... secret) {
        this.keystore = keystore;
        this.secret = secret.clone();
    }

    protected KeyStoreLeveragingFeature() {
        // for JCommander
    }

    protected KeyStoreHandler getStorage() {
        return storage;
    }

    @Override
    public void setup() throws SetupFailedException {
        try {
            if (keystore.exists()) {
                storage = KeyStoreHandler.open(keystore, secret);
            } else {
                storage = KeyStoreHandler.create(keystore, secret);
            }
        } catch (final Exception ex) {
            throw new SetupFailedException(ex);
        }
    }

    protected void test(final char... pwd) throws TestFailedException {
        try {
            KeyStoreHandler.open(keystore, pwd);
        } catch (final Exception ex) {
            throw new TestFailedException(ex);
        }
    }

    @Override
    public void test() throws TestFailedException {
        test(secret);
    }
}
