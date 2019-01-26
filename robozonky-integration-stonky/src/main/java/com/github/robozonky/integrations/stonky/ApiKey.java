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

package com.github.robozonky.integrations.stonky;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.github.robozonky.internal.api.Defaults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * In order to communicate with the Google Sheets API, we need to authenticate against it. For that, we need to use a
 * secret. That secret can be obtained from Google's developer console, and should not be shared with anyone.
 * Unfortunately, there's no way to have RoboZonky work with the API without disclosing the secret key. This class at
 * least tries to obfuscate the key instead of disclosing it in plain text.
 * <p>
 * For that reason, this class has no comments and doesn't do anything to help attackers figure out what it does. We
 * are aware that this is not security, but it is the best we can do about this particular situation. If we find out
 * that the API secret is being used by clients other than RoboZonky, that secret will be changed. Worst case, the
 * entire Stonky integration may be removed without replacement.
 */
class ApiKey {

    private static final char[] ONE = "tgkT6AaJ31xVig1s".toCharArray();
    private static final byte[] TWO = "RoboZonky".getBytes();
    /**
     * Obtained by running {@link #main(String...)} on the JSON secret provided by Google.
     */
    private static final String RESULT = "ka6y1FmftPjeAToV:IzqU6mqiqgOi/6w5OnHBzZeEmONtzEH765+7VFGZgWhqGT6Fboe9s" +
            "FKOBsYBn/wYDxWbkwjxeCxbBrF0pDxnRr/Zx4DYlHGqNL4E0+ITARAUZEmJnfcTwQD3bT543WHzFvT2sDpKaRogmaYG8Y2WiweJ" +
            "Ak9Oh/WjzkgMi6/lfyTYvAOZWSz3UwBaI/pHMmB6pv8WUbLSZfXsvgIvyFTbMXZ7lS2WmtX+2hJ0ScOj25Wsa2EvgafQZcTxybm" +
            "627JbuehQ8KeRaO5Y+HFjhRxgc45nD/iWQtqmLc7PHagQxUQbHRNBwx/wyKOyQzqfOqMBs48frwgBqFdYkGFwqB6YfLH6gSJPH6" +
            "AiEbZf8mGwVRUz+Dul9VEpoZcxhm5mnQgz69GGP6F3oRJDd4ZNqquyXfOK2CwpuYE4ZkQFd8yMyfEYbc3hJtAzWXAy9nUB3tLL+" +
            "ZL5vOGtCr8BFuwGh+udsZPp+mdkvM7HxJoM6T5DbrF6X3VT4i5ULy2RlvOym2HO2Rt0VrlATEFSn7InDX/EDavukoQ6L/7bGWCH" +
            "m9q0RyQM3Zd2YOKbXL54T3zJjoVXaaa+5wvslURZ9nGbGT4Ucj+mf30=";
    private static final Logger LOGGER = LogManager.getLogger(ApiKey.class);

    public static void main(final String... args) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidKeySpecException,
            InvalidAlgorithmParameterException {
        final byte[] input = Files.readAllBytes(new File(args[0]).toPath());
        final String encrypted = new String(encrypt(input));
        LOGGER.info("Encrypted: '{}'.", encrypted);
        final String decrypted = new String(decrypt(encrypted));
        LOGGER.info("Decrypted: '{}'.", decrypted);
    }

    static byte[] encrypt(final byte[] input) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeySpecException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        final Cipher cipher = getCipher();
        cipher.init(Cipher.ENCRYPT_MODE, createSecretKey());
        final byte[] cryptoText = cipher.doFinal(input);
        final byte[] iv = cipher.getIV();
        final String result = base64Encode(iv) + ":" + base64Encode(cryptoText);
        return result.getBytes(Defaults.CHARSET);
    }

    public static byte[] get() {
        try {
            return decrypt(RESULT);
        } catch (final GeneralSecurityException ex) {
            throw new IllegalStateException("Failed reading API key.", ex);
        }
    }

    private static SecretKeySpec createSecretKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        final PBEKeySpec keySpec = new PBEKeySpec(ONE, TWO, 40_000, 128);
        final SecretKey keyTmp = keyFactory.generateSecret(keySpec);
        return new SecretKeySpec(keyTmp.getEncoded(), "AES");
    }

    private static Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance("AES/GCM/NoPadding");
    }

    private static String base64Encode(final byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    static byte[] decrypt(final String string) throws InvalidKeySpecException, NoSuchAlgorithmException,
            NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        final String iv = string.split(":")[0];
        final String property = string.split(":")[1];
        final Cipher cipher = getCipher();
        cipher.init(Cipher.DECRYPT_MODE, createSecretKey(), new GCMParameterSpec(128, base64Decode(iv)));
        return cipher.doFinal(base64Decode(property));
    }

    private static byte[] base64Decode(final String property) {
        return Base64.getDecoder().decode(property);
    }
}
