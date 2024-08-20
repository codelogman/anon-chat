package com.node.gost;

import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ulDrawHelper {




    public static String DES_DEncryption(String data) {
        Cipher ecipher;

        // Create a new DES key based on the 8 bytes in the secretKey array
        byte[] keyData = {(byte) 0x51, (byte) 0x33, (byte) 0x18, (byte) 0x79,
                (byte) 0x62, (byte) 0x11, (byte) 0x73, (byte) 0x19};

        try {

            byte[] cipherBytes = fromBase64(data);

            SecretKeySpec key = new SecretKeySpec(keyData, "DES/CFB8/NoPadding");

            // Setup the Initialization vector.
            byte[] iv1 = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

            AlgorithmParameterSpec paramSpec = new IvParameterSpec(keyData);

            ecipher = Cipher.getInstance("DES/CFB8/NoPadding");

            ecipher.init(Cipher.DECRYPT_MODE, key, paramSpec);

            byte[] output = ecipher.doFinal(cipherBytes);

            String PlainrStr = new String(output);

            return PlainrStr;

        } catch (GeneralSecurityException e) {
            return data;
            //throw new RuntimeException(e);
        } catch (IllegalArgumentException b) {
            return data;
        }
    }

    public static byte[] fromBase64(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
    }

    public static String DES_Encryption(String text)

    {

        Cipher ecipher;

        // Create a new DES key based on the 8 bytes in the secretKey array
        byte[] keyData = {(byte) 0x51, (byte) 0x33, (byte) 0x18, (byte) 0x79,
                (byte) 0x62, (byte) 0x11, (byte) 0x73, (byte) 0x19};

        try {

            SecretKeySpec key = new SecretKeySpec(keyData, "DES/CFB8/NoPadding");

            byte[] data = text.getBytes();

            // Setup the Initialization vector.
            byte[] iv1 = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

            AlgorithmParameterSpec paramSpec = new IvParameterSpec(keyData);

            ecipher = Cipher.getInstance("DES/CFB8/NoPadding");

            ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);

            byte[] output = ecipher.doFinal(data);


            return toBase64(output);

        } catch (GeneralSecurityException e) {
            return text;
            //throw new RuntimeException(e);
        } catch (Exception b) {
            return text;
        }
    }

    public static String toBase64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP).trim();
    }

}
