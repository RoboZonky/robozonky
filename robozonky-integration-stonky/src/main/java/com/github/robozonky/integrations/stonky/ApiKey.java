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

package com.github.robozonky.integrations.stonky;

import java.io.File;
import java.nio.file.Files;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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
    private static final String RESULT = "kDbUDhMpf3zyt+ybZMSXWw==:SPuxElMuJ+gZq1qFB+DO6/qg4Y85Gg/idMIGqYrpoh0dxxckj" +
            "VORe1ccun5leffFJPG1A3M3YscKJ7i/5sKtw7F4wwtg4CZdMBx4ikTmdHAOnoT8+WLes6JUI6o2WPrh+2ysWTDa/V54nMK2px6/nVrq" +
            "Z7QQJcYeVmpsDeqtpBSov1J7S/4GqRFc24sibhtrDdZuLoZmFhqFHqXNIhEdbVs/0RQx6RYxQcPQ3r8RyfYoY+qONdrCwoOTR3sr4Jr" +
            "/zvxm9x2a5spXqTxOOPTpXkzxYqEnCAYmeXGQW5JxNRiasNw0EU8pp6GFNz34kBSwCTjYVi2WfVqM67rKJrL3wzUS9C+Gv1cjo0g48Y" +
            "WANUa27fyuOPWk4XNzDmhtn62PZIbeg523mNDTAtKQ4HrS7abp040l2LsmGuUjLKkt3FtQLgB1F/qbn2BNqtE2nV/pWUH7UcxnNIWH3" +
            "4VKcQ8Ch7isExvpber+foq2aq2OybtARE34Dqwp39EroWTwsaa1kPr62eMxFynYz8TgJxOlnrDmZ44K8DjXs860pGfvHNma5VgNm9uM" +
            "pncJ2LT+xO4ybDH27dDVK3ZyK4HXxbyJkA==";

    public static void main(final String... args) throws Exception {
        final byte[] input = Files.readAllBytes(new File(args[0]).toPath());
        final SecretKeySpec key = createSecretKey();
        System.out.println("Original password: " + new String(input));
        final String encryptedPassword = encrypt(input, key);
        System.out.println("Encrypted password: " + encryptedPassword);
        final byte[] decryptedPassword = decrypt(encryptedPassword, key);
        System.out.println("Decrypted password: " + new String(decryptedPassword));
    }

    public static byte[] get() throws GeneralSecurityException {
        return decrypt(RESULT, createSecretKey());
    }

    private static SecretKeySpec createSecretKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        final PBEKeySpec keySpec = new PBEKeySpec(ONE, TWO, 40_000, 128);
        final SecretKey keyTmp = keyFactory.generateSecret(keySpec);
        return new SecretKeySpec(keyTmp.getEncoded(), "AES");
    }

    private static String encrypt(final byte[] property, final SecretKeySpec key) throws GeneralSecurityException {
        final Cipher pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key);
        final AlgorithmParameters parameters = pbeCipher.getParameters();
        final IvParameterSpec ivParameterSpec = parameters.getParameterSpec(IvParameterSpec.class);
        final byte[] cryptoText = pbeCipher.doFinal(property);
        final byte[] iv = ivParameterSpec.getIV();
        return base64Encode(iv) + ":" + base64Encode(cryptoText);
    }

    private static String base64Encode(final byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] decrypt(final String string, final SecretKeySpec key) throws GeneralSecurityException {
        final String iv = string.split(":")[0];
        final String property = string.split(":")[1];
        final Cipher pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(base64Decode(iv)));
        return pbeCipher.doFinal(base64Decode(property));
    }

    private static byte[] base64Decode(final String property) {
        return Base64.getDecoder().decode(property);
    }
}
