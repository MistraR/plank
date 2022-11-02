package com.mistra.plank.common.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;

public class RSAUtil {

    private static byte[] encodeWithPublicKey2(String str, String publicKey) {
        try {
            byte[] keyBytes = Base64.decodeBase64(publicKey);
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));

            int maxBitLength = pubKey.getModulus().bitLength() / 8 - 11;

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);

            byte[] data = str.getBytes(StandardCharsets.UTF_8);
            byte[] encodeBytes = null;
            for (int i = 0; i < data.length; i += maxBitLength) {
                byte[] doFinal = cipher.doFinal(ArrayUtils.subarray(data, i, i + maxBitLength));
                encodeBytes = ArrayUtils.addAll(encodeBytes, doFinal);
            }
            return encodeBytes;
        } catch (RuntimeException | InvalidKeySpecException | NoSuchAlgorithmException | IllegalBlockSizeException
                | BadPaddingException | InvalidKeyException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String encodeWithPublicKey(String str, String publicKey) {
        return Base64.encodeBase64String(encodeWithPublicKey2(str, publicKey));
    }

}
