package ai.nextgpu.common.model.util;

import ai.nextgpu.common.util.StringUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

public class StringUtilTest {

    /**
     * Tests for the generateRandomString method in the StringUtil class.
     * This method generates a random string of a specified length, with options
     * to include uppercase letters, lowercase letters, digits, and special characters.
     */

    @Test
    void testGenerateRandomString_withAllCharacterTypes() {
        int length = 10;
        String result = StringUtil.generateRandomString(length, true, true, true, true);
        assertNotNull(result, "Generated string should not be null.");
        assertEquals(length, result.length(), "Generated string should match the requested length.");
        assertTrue(result.matches(".*[A-Z].*"), "Generated string should contain at least one uppercase letter.");
        assertTrue(result.matches(".*[a-z].*"), "Generated string should contain at least one lowercase letter.");
        assertTrue(result.matches(".*\\d.*"), "Generated string should contain at least one digit.");
        assertTrue(result.matches(".*[!@#$%^&*()_+{}\\[\\]\\\\|:;<>,./?].*"), "Generated string should contain at least one special character.");
    }

    @Test
    void testGenerateRandomString_withUpperCaseOnly() {
        int length = 8;
        String result = StringUtil.generateRandomString(length, true, false, false, false);
        assertNotNull(result, "Generated string should not be null.");
        assertEquals(length, result.length(), "Generated string should match the requested length.");
        assertTrue(result.matches("[A-Z]+"), "Generated string should contain only uppercase letters.");
    }

    @Test
    void testGenerateRandomString_withLowerCaseOnly() {
        int length = 12;
        String result = StringUtil.generateRandomString(length, false, true, false, false);
        assertNotNull(result, "Generated string should not be null.");
        assertEquals(length, result.length(), "Generated string should match the requested length.");
        assertTrue(result.matches("[a-z]+"), "Generated string should contain only lowercase letters.");
    }

    @Test
    void testGenerateRandomString_withDigitsOnly() {
        int length = 6;
        String result = StringUtil.generateRandomString(length, false, false, true, false);
        assertNotNull(result, "Generated string should not be null.");
        assertEquals(length, result.length(), "Generated string should match the requested length.");
        assertTrue(result.matches("\\d+"), "Generated string should contain only digits.");
    }

    @Test
    void testGenerateRandomString_withSpecialCharactersOnly() {
        int length = 5;
        String result = StringUtil.generateRandomString(length, false, false, false, true);
        assertNotNull(result, "Generated string should not be null.");
        assertEquals(length, result.length(), "Generated string should match the requested length.");
        assertTrue(result.matches("[!@#$%^&*()_+{}\\[\\]\\\\|:;<>,./?]+"), "Generated string should contain only special characters.");
    }

    @Test
    void testGenerateRandomString_withNoCharacterTypesSelected() {
        int length = 10;
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                StringUtil.generateRandomString(length, false, false, false, false));
        assertEquals("chars length must be greater than 0", exception.getMessage(), "Exception message should match.");
    }
}
