package com.pgmate.pay.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptUtil {
	public static final String CHARSET_UTF8 = "UTF-8";
    public static final String DEFAULT_CHARSET = CHARSET_UTF8;
    
    public static String sha256(String valueStr) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        digest.update(valueStr.getBytes(DEFAULT_CHARSET));

        return bytesToHex(digest.digest());
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
