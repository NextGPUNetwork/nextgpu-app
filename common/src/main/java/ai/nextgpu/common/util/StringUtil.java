package ai.nextgpu.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Base64;
import java.util.Random;

/**
 * A utility class for generating and managing strings with various configurations.
 */
@Component
public class StringUtil {

    private static final Logger log = LoggerFactory.getLogger(StringUtil.class);

    private static final String BASE36_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Generates a random string of the specified length. The generated string includes uppercase letters,
     * lowercase letters, digits, and special characters by default.
     *
     * @param length the length of the random string to be generated
     * @return a randomly generated string of the specified length
     */
    public static String generateRandomString(int length) {
        return generateRandomString(length, true, true, true, true);
    }

    /**
     * Generates a random alphanumeric string of the specified length.
     * The generated string includes uppercase letters, lowercase letters, and digits.
     *
     * @param length the length of the random alphanumeric string to be generated
     * @return a randomly generated alphanumeric string of the specified length
     */
    public static String generateRandomAlphaNumericString(int length) {
        return generateRandomString(length, true, true, true, false);
    }

    /**
     * Generates a random numeric string of the specified length. The generated string
     * includes only numeric digits (0-9).
     *
     * @param length the length of the random numeric string to be generated
     * @return a randomly generated numeric string of the specified length
     */
    public static String generateRandomNumericString(int length) {
        return generateRandomString(length, false, false, true, false);
    }

    /**
     * Generates a random string of the specified length with a customizable set of character types.
     * The character types to include can be configured using the boolean parameters.
     *
     * @param length the length of the random string to be generated
     * @param includeUpperCase whether to include uppercase letters in the string (A-Z)
     * @param includeLowerCase whether to include lowercase letters in the string (a-z)
     * @param includeDigits whether to include digits in the string (0-9)
     * @param includeSpecialCharacters whether to include special characters in the string (!@#$%^&*()_+{}[]\|:;<>,./?)
     * @return a randomly generated string of the specified length containing the specified types of characters
     * @throws IllegalArgumentException if no character types are selected for inclusion
     */
    public static String generateRandomString(int length, boolean includeUpperCase, boolean includeLowerCase, boolean includeDigits, boolean includeSpecialCharacters) {
        Random random = new Random();
        StringBuilder randomCode = new StringBuilder();
        StringBuilder chars = new StringBuilder();
        if (includeUpperCase) {
            chars.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }
        if (includeLowerCase) {
            chars.append("abcdefghijklmnopqrstuvwxyz");
        }
        if (includeDigits) {
            chars.append("0123456789");
        }
        if (includeSpecialCharacters) {
            chars.append("!@#$%^&*()_+{}[]\\|:;<>,./?");
        }
        for (int i = 0; i < length; i++) {
            randomCode.append(chars.charAt(random.nextInt(chars.length())));
        }
        return randomCode.toString();
    }

    /**
     * Generates a random hexadecimal string of the specified length.
     *
     * @param length the length of the random hexadecimal string to generate
     * @return a randomly generated hexadecimal string with the specified length
     */
    public static String generateRandomHexString(int length) {
        Random random = new Random();
        StringBuilder randomCode = new StringBuilder();
        String chars = "0123456789ABCDEF";
        for (int i = 0; i < length; i++) {
            randomCode.append(chars.charAt(random.nextInt(chars.length())));
        }
        return randomCode.toString();
    }

    private static String toBaseN(java.math.BigInteger number, String charset) {
        StringBuilder result = new StringBuilder();
        BigInteger base = BigInteger.valueOf(charset.length());
        while (number.compareTo(BigInteger.ZERO) > 0) {
            result.insert(0, charset.charAt(number.mod(base).intValue()));
            number = number.divide(base);
        }
        return result.length() == 0 ? "0" : result.toString();
    }

    /**
     * Converts a hexadecimal string to an alphanumeric string using a target character set.
     * Depending on the caseSensitive flag, the conversion output can use either a case-sensitive
     * or case-insensitive character set.
     *
     * @param hexString the input hexadecimal string to be converted
     * @param caseSensitive a flag indicating whether the conversion should use a case-sensitive
     *                      character set (e.g., BASE62) or a case-insensitive character set (e.g., BASE36)
     * @return the equivalent alphanumeric string if conversion is successful, or an empty string
     *         if the input is null, empty, or invalid
     */
    public static String hexStringToAlphanumeric(String hexString, boolean caseSensitive) {
        if (hexString == null || hexString.isEmpty()) {
            return "";
        }
        try {
            BigInteger number = new BigInteger(hexString, 16);
            return toBaseN(number, caseSensitive ? BASE62_CHARS : BASE36_CHARS);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /**
     * Validates whether the provided string is a valid JSON.
     *
     * @param json the string to be validated as JSON
     * @return true if the string is a valid JSON, false otherwise
     */
    public static boolean isValidJson(String json){
        try {
            new JSONObject(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static byte[] base64ToBytes(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    public static String bytesToBase64(byte[] byteArray) {
        return Base64.getEncoder().encodeToString(byteArray);
    }

    public static double getLevenshteinDistance(String a, String b) {
        if (a == null || a.isBlank() || b == null || b.isBlank()) return 0.0;
        if (a.equals(b)) return 1.0;

        return LevenshteinDistance.getDefaultInstance().apply(a, b);
    }

    public static double getJaroWinklerDistance(String a, String b) {
        if (a == null || a.isBlank() || b == null || b.isBlank()) return 0.0;
        if (a.equals(b)) return 1.0;
        return new JaroWinklerDistance().apply(a, b);
    }
}
