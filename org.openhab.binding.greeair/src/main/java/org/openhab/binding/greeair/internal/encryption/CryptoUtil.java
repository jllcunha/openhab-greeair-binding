/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.greeair.internal.encryption;

import java.security.Key;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.openhab.binding.greeair.handler.GreeAirHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CryptoUtil class provides functionality for encrypting and decrypting
 * messages sent to and from the Air Conditioner
 *
 * @author John Cunha - Initial contribution
 */

public class CryptoUtil {
    static private final Logger logger = LoggerFactory.getLogger(GreeAirHandler.class);
    static String AES_General_Key = "a3K8Bx%2r8Y7#xDh";

    public static String GetAESGeneralKey() {
        return AES_General_Key;
    }

    public static byte[] GetAESGeneralKeyByteArray() {
        return AES_General_Key.getBytes();
    }

    public static String decryptPack(byte[] keyarray, String message) throws Exception {
        String descrytpedMessage = null;
        try {
            Key key = new SecretKeySpec(keyarray, "AES");
            // BASE64Decoder decoder = new BASE64Decoder();
            Base64.Decoder decoder = Base64.getDecoder();
            // Decoder decoder = new Decoder();
            byte[] imageByte = decoder.decode(message);
            // byte[] imageByte = decoder.decodeBuffer(message);

            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.DECRYPT_MODE, key);
            byte[] bytePlainText = aesCipher.doFinal(imageByte);

            descrytpedMessage = new String(bytePlainText);
        } catch (Exception ex) {
            logger.debug("Greeair failed to decrypt the recieved data due to {} ", ex.getMessage());
        }
        return descrytpedMessage;
    }

    public static String encryptPack(byte[] keyarray, String message) throws Exception {
        String encrytpedMessage = null;

        try {
            Key key = new SecretKeySpec(keyarray, "AES");
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] bytePlainText = aesCipher.doFinal(message.getBytes());

            Base64.Encoder newencoder = Base64.getEncoder();
            encrytpedMessage = new String(newencoder.encode(bytePlainText));
            encrytpedMessage = encrytpedMessage.substring(0, encrytpedMessage.length());
        } catch (Exception ex) {
            logger.debug("Greeair failed to encrypt the data to send to device due to {} ", ex.getMessage());
        }
        return encrytpedMessage;
    }
}
