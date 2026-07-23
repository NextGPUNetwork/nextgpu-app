package ai.nextgpu.common.util;

import org.apache.commons.text.similarity.CosineSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
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

    /**
     * Converts a BigInteger to a string representation in a specified base using the provided charset.
     * This is a helper method for base conversion operations.
     *
     * @param number the BigInteger number to convert
     * @param charset the character set to use for the base representation
     * @return the string representation of the number in the specified base
     */
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

    /**
     * Validates whether the provided string is an IPv4 or IPv6 address literal.
     *
     * @param ipAddress the string to validate
     * @return true if the string is a valid IP address literal, false otherwise
     */
    public static boolean isValidIPAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        String trimmed = ipAddress.trim();
        if (!trimmed.equals(ipAddress)) {
            return false;
        }
        if (trimmed.contains(".")) {
            return isValidIPv4Address(trimmed);
        }
        if (trimmed.contains(":")) {
            return isValidIPv6Address(trimmed);
        }
        return false;
    }

    private static boolean isValidIPv4Address(String ipAddress) {
        int parts = 0;
        int partValue = 0;
        int partLength = 0;

        for (int i = 0; i < ipAddress.length(); i++) {
            char c = ipAddress.charAt(i);
            if (c == '.') {
                if (partLength == 0 || partValue > 255) {
                    return false;
                }
                parts++;
                partValue = 0;
                partLength = 0;
                continue;
            }
            if (!Character.isDigit(c)) {
                return false;
            }
            partValue = (partValue * 10) + Character.digit(c, 10);
            partLength++;
            if (partLength > 3 || partValue > 255) {
                return false;
            }
        }
        return parts == 3 && partLength > 0 && partValue <= 255;
    }

    private static boolean isValidIPv6Address(String ipAddress) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            return address instanceof java.net.Inet6Address;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Decodes a Base64-encoded string to a byte array.
     *
     * @param base64String the Base64-encoded string to decode
     * @return the decoded byte array
     */
    public static byte[] base64ToBytes(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    /**
     * Encodes a byte array to a Base64-encoded string.
     *
     * @param byteArray the byte array to encode
     * @return the Base64-encoded string
     */
    public static String bytesToBase64(byte[] byteArray) {
        return Base64.getEncoder().encodeToString(byteArray);
    }

    /**
     * Calculates the Levenshtein distance between two strings, representing the minimum number
     * of single-character edits (insertions, deletions, substitutions) required to change one
     * string into the other. Returns 0.0 for null or blank inputs, 1.0 for identical strings.
     *
     * @param a the first string to compare
     * @param b the second string to compare
     * @return the Levenshtein distance as a double value
     */
    public static double getLevenshteinDistance(String a, String b) {
        if (a == null || a.isBlank() || b == null || b.isBlank()) return 0.0;
        if (a.equals(b)) return 1.0;

        return LevenshteinDistance.getDefaultInstance().apply(a, b);
    }

    /**
     * Calculates the Jaro-Winkler distance between two strings, a string similarity metric that
     * gives more favorable ratings to strings with common prefixes. The distance ranges from 0.0
     * (no similarity) to 1.0 (identical strings). Returns 0.0 for null or blank inputs.
     *
     * @param a the first string to compare
     * @param b the second string to compare
     * @return the Jaro-Winkler distance as a double value between 0.0 and 1.0
     */
    public static double getJaroWinklerDistance(String a, String b) {
        if (a == null || a.isBlank() || b == null || b.isBlank()) return 0.0;
        if (a.equals(b)) return 1.0;
        return new JaroWinklerDistance().apply(a, b);
    }

    /**
     * Computes the cosine similarity between two strings using term frequencies.
     * The strings are tokenized on whitespace after converting to lowercase.
     * Cosine similarity ranges from: 1.0 = identical token distribution to 0.0 = no tokens in common
     *
     * @param a First string.
     * @param b Second string.
     * @return Cosine similarity in the range [0.0, 1.0].
     */
    public static double getCosineSimilarity(String a, String b) {
        if (a == null || b == null) {
            return 0.0;
        }
        Map<CharSequence, Integer> leftVector = new HashMap<>();
        Map<CharSequence, Integer> rightVector = new HashMap<>();

        for (String token : a.toLowerCase().split("\\s+")) {
            leftVector.merge(token, 1, Integer::sum);
        }
        for (String token : b.toLowerCase().split("\\s+")) {
            rightVector.merge(token, 1, Integer::sum);
        }
        return new CosineSimilarity().cosineSimilarity(leftVector, rightVector);
    }
}
