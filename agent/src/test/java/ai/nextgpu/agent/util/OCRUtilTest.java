package ai.nextgpu.agent.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

public class OCRUtilTest {
    private static final Logger log = LoggerFactory.getLogger(OCRUtilTest.class);

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("RapidOCR - null file input should return null without throwing")
    void testRapidOCR_NullFile() {
        assertDoesNotThrow(() -> {
            String result = OCRUtil.imageRapidOCR(null);
            assertNull(result, "Result should be null for null file");
        });
    }

    @Test
    @DisplayName("RapidOCR - non-existent file input should return null without throwing")
    void testRapidOCR_NonExistentFile() {
        File nonExistentFile = new File("non_existent_image.jpg");

        assertDoesNotThrow(() -> {
            String result = OCRUtil.imageRapidOCR(nonExistentFile);
            assertNull(result, "Result should be null for non-existent file");
        });
    }

    @Test
    @DisplayName("RapidOCR - ocrimg1.jpg should recognize Marlboro cigarette advertisement text")
    void testRapidOCR_Image1_WithExpectedText() throws Exception {
        File file = new File(getClass().getClassLoader().getResource("attachments/ocrimg1.jpg").getFile());
        String result = OCRUtil.imageRapidOCR(file);
        log.debug(result);

        assertNotNull(result, "RapidOCR result must not be null");

        String expected = "try Marlb oro\n" +
                "\n" +
                "-the filter cigarette with the unfiltered taste\n" +
                "If you think flavor went out when filters came in, you've got\n" +
                "another smoke coming. Make it Marlboro. This one delivers\n" +
                "the goods on flavor. Always has. That famous Marlboro\n" +
                "Filter-Flavor recipe gives a man a lot of flavor to draw on-\n" +
                "and mighty easy drawing it is.\n" +
                "Sort of nice to know a cigarette so good can be so comfort-\n" +
                "able to smoke through Marlboro's exclusive Selectrate filter.";

        double similarity = computeSimilarity(result, expected);
        log.debug("OCR similarity score: {}%", similarity * 100);
        log.debug(result);
        log.debug(expected);


        assertTrue(similarity >= 0.70,
                String.format("Expected at least 80%% similarity but got %.2f%%", similarity * 100));
    }

    @Test
    @DisplayName("RapidOCR - ocrimg2.jpg should recognize Miami Grand Prix email text")
    void testRapidOCR_Image2_WithExpectedText() throws Exception {
        File file = new File(getClass().getClassLoader().getResource("attachments/ocrimg2.jpg").getFile());
        String result = OCRUtil.imageRapidOCR(file);
        log.debug(result);

        assertNotNull(result, "RapidOCR result must not be null");

        String expected = "2070106870A\n" +
                "\n" +
                "----- Original Message-\n" +
                "From:\n" +
                "\n" +
                "Arce, Miguel\n" +
                "\n" +
                "Sent\n" +
                "\n" +
                "Monday, November 29, 1999 12:37 PM\n" +
                "\n" +
                "To:\n" +
                "\n" +
                "Bayliss, Elissa L.\n" +
                "\n" +
                "Cc:\n" +
                "\n" +
                "Murillo, Joe; Garcia, Elisa\n" +
                "\n" +
                "Subject:\n" +
                "\n" +
                "MIAMI GRAND PRIX RACING SUBJECTS\n" +
                "\n" +
                "Importance:\n" +
                "\n" +
                "High\n" +
                "\n" +
                "Elissa:\n" +
                "As per your instructions of this morning attached please find samples of what was done with Indy transparencies\n" +
                "concerning last years Miami Grand Prix promotion in Latin America. As described to you on my note of the 23rd, we would\n" +
                "like to use new photos for posters, banners, counter displays and print (in Latin America only; Mexico, Central America,\n" +
                "Colombia, Ecuador and the Dominican Republic). Please reply as soon as possible as this is a time sensitive issue for us.\n" +
                "Thank you for your help and support.\n" +
                "Best regards,\n" +
                "Miguel";

        double similarity = computeSimilarity(result, expected);
        log.debug("OCR similarity score: {}%", similarity * 100);
        log.debug(result);
        log.debug(expected);


        assertTrue(similarity >= 0.70,
                String.format("Expected at least 80%% similarity but got %.2f%%", similarity * 100));
    }

    @Test
    @DisplayName("RapidOCR - ocrimg3.jpg should recognize Frank Hsu internal email text")
    void testRapidOCR_Image3_WithExpectedText() throws Exception {
        File file = new File(getClass().getClassLoader().getResource("attachments/ocrimg3.jpg").getFile());
        String result = OCRUtil.imageRapidOCR(file);
        log.debug(result);

        assertNotNull(result, "RapidOCR result must not be null");

        String expected = "Ellis, Cathy L. (WSA)\n" +
                "From:\n" +
                "\n" +
                "Hsu, Frank\n" +
                "\n" +
                "Sent:\n" +
                "\n" +
                "Wednesday, March 24, 1999 10:58 AM\n" +
                "\n" +
                "To:\n" +
                "\n" +
                "Ellis, Cathy L. (WSA)\n" +
                "\n" +
                "Subject:\n" +
                "\n" +
                "RE: PLease review asap\n" +
                "\n" +
                "2063075727\n" +
                "\n" +
                "Cathy,\n" +
                "My comments are in blue. It looks good !\n" +
                "Frank\n" +
                "\n" +
                "AMNCIGTCORESTA-h\n" +
                "su.doe";

        double similarity = computeSimilarity(result, expected);
        log.debug("OCR similarity score: {}%", similarity * 100);
        log.debug(result);
        log.debug(expected);


        assertTrue(similarity >= 0.70,
                String.format("Expected at least 80%% similarity but got %.2f%%", similarity * 100));
    }

    @Test
    @DisplayName("RapidOCR - ocrimg4.jpg should recognize BAT legal department letter text")
    void testRapidOCR_Image4_WithExpectedText() throws Exception {
        File file = new File(getClass().getClassLoader().getResource("attachments/ocrimg4.jpg").getFile());
        String result = OCRUtil.imageRapidOCR(file);
        log.debug(result);

        assertNotNull(result, "RapidOCR result must not be null");

        String expected = "B.A.T\n" +
                "BRITISH-AMERICAN TOBACCO COMPANY LTD\n" +
                "A member of the B.A.T Industries Group\n" +
                "\n" +
                "Pyolnents\n" +
                "\n" +
                "Registered Office:\n" +
                "\n" +
                "Legal Department\n" +
                "\n" +
                "PO Box 482 Westminster House\n" +
                "\n" +
                "Solicitors: N.B. Cannar LL.B.\n" +
                "\n" +
                "7 Millbank London SWIP 3JE\n" +
                "\n" +
                "A. Johnson LL.B.\n" +
                "\n" +
                "Telephone: 01-222 I222\n" +
                "\n" +
                "P.C. Godby M.A.\n" +
                "\n" +
                "Telex: 27384 BATTOB G*\n" +
                "\n" +
                "P.R. Sassoon B.A.\n" +
                "\n" +
                "Facsimile No: 01-222 3659\n" +
                "\n" +
                "Your Ref .:\n" +
                "RD 163\n" +
                "\n" +
                "27th July 19890\n" +
                "\n" +
                "Our Ref .:\n" +
                "\n" +
                "C-047-a\n" +
                "\n" +
                "C. G. Lamb, Esq.,\n" +
                "Brown & Williamson Tobacco Corporation,\n" +
                "P.O. Box 35090,\n" +
                "Louisville,\n" +
                "Kentucky 40232,\n" +
                "U.S.A.\n" +
                "\n" +
                "Dear Chuck,\n" +
                "\n" +
                "Tobacco Smoke Filters\n" +
                "I am writing to inform you that it has been decided that\n" +
                "BATCo no longer wish to maintain the above case in Australia,\n" +
                "Belgium, Finland, Germany, Holland, South Africa, Switzerland\n" +
                "and the United Kingdom and I have been asked to offer these\n" +
                "Patents to you by way of assignment.\n" +
                "\n" +
                "o u wtested ASK DES\n" +
                "Enclosed is a copy of the corresponding U.S.A. patent for\n" +
                "your information and I look forward to hearing from you on this\n" +
                "matter in due course.\n" +
                "\n" +
                "With kind regards,\n" +
                "Yours sincerely,\n" +
                "\n" +
                "RECEIVE\n" +
                "\n" +
                "Debbie Byard\n" +
                "\n" +
                "AUG O.S 1989\n" +
                "P & T\n" +
                "\n" +
                "681649618\n" +
                "\n" +
                "Incorporated in London Number 74974 Cables: Vehicular London SWI";

        double similarity = computeSimilarity(result, expected);
        log.debug("OCR similarity score: {}%", similarity * 100);
        log.debug(result);
        log.debug(expected);


        assertTrue(similarity >= 0.70,
                String.format("Expected at least 80%% similarity but got %.2f%%", similarity * 100));
    }

    @Test
    @DisplayName("RapidOCR - ocrimg5.jpg should recognize text matching ocrimg5_expected.txt")
    void testRapidOCR_Image5_WithExpectedText() throws Exception {
        File file = new File(getClass().getClassLoader().getResource("attachments/ocrimg5.jpg").getFile());
        String result = OCRUtil.imageRapidOCR(file);
        log.debug(result);

        assertNotNull(result, "RapidOCR result must not be null");

        String expected = "\"Among criticisms made by scientists of this study are:\n" +
                "\"1. A mere numerical association such as Drs. Hammond and Horn\n" +
                "claim does not establish any cause and effect relationship.\n" +
                "\"2. The survey is limited to smoking habits and therefore does\n" +
                "not rule out any number of possible other factors in present-day living\n" +
                "that may influence death rates.\n" +
                "\"3. The survey is shown to be non-representative of the na-\n" +
                "tional population.\n" +
                "\"4. The small number of deaths from lung cancer -- at most,\n" +
                "285 out of 8,105 deaths -- makes questionable any conclusions drawn, es-\n" +
                "pecially since the sampling methods of the survey are not statistically\n" +
                "sound.\n" +
                "\n" +
                "\"Statistical studies of this nature can at best indicate areas\n" +
                "that require study. This has been done, and scientists devoted to such\n" +
                "study have recently reported their inability to identify any cancer-\n" +
                "causing substance in tobacco smoke derivatives.\n" +
                "\"It would be tragic if an over-publicized allegation that lacks\n" +
                "scientific support were to divert and impede public or private support of\n" +
                "sound research in such an important field of health.\"\n" +
                "01138186\n" +
                "\n" +
                "Cancer Scare, Production Increase\n" +
                "\n" +
                "Most of the funds for this wor\n" +
                "being contributedi by the majc\n" +
                "vigarette manufacturing compar\n" +
                "\n" +
                "Reduce Domestic Cigarette Sales\n" +
                "\n" +
                "ies. Tobacco Associates and mos\n" +
                "of the other producers and Wart\n" +
                "house organizationa in the cl\n" +
                "arette tobacco producing state\n" +
                "\n" +
                "A*velopment within the Unit- bers of a\n" +
                "\n" +
                "are making contributions.\n" +
                "Scientific Advisory and other potential itritants. Tis?\n" +
                "ed States which directly affects Board. This Board determines the sues of special! interest. are those\n" +
                "the gale of our tobacco this year Scope and direction. of the re- of the mouth, lungs, glands, heart\n" +
                "is mainly the further decline in search program, solicits and re- and other organs of subjects of\n" +
                "the: domestic consumption of cig- views requests for resarch granta various ageb, sex and. atrains.\n" +
                "arettes due to the lung cancer from universities, hospitals, and\n" +
                "acare and other adverse factors other recognized research organ-\n" +
                "Smoking and other tobacco ha-\n" +
                "major tohacro companies have izations. The Board itself does not\n" +
                "f bits, and the emotional and phy-\n" +
                "\n" +
                "JOURNAL-HERALD\n" +
                "\n" +
                "announcedi\n" +
                "\n" +
                "undertake research although Indi-\n" +
                "a ;. sical makeup of smokers, with re-\n" +
                "Tobacco Committee chairman, vidual members may head pro-\n" +
                "spect to establishment, duration\n" +
                "\n" +
                "Waycross, Ga.\n" +
                "\n" +
                "E. Kontz Bennett discussing out- jects in the institutions. with and intensity of tobacco use, and\n" +
                "Jook for the 1955 auction here and which they are: affiliated ..\n" +
                "\n" +
                "Juily 12, 1955\n" +
                "\n" +
                "correlation of these data with: me-\n" +
                "\n" +
                "ughout th ^belt quote\n" +
                "\n" +
                "re.\n" +
                "This Scientific: Advisory Board\n" +
                "tabolic, glandular and nervous\n" +
                "decided that the objectives of the\n" +
                "types under various: degrees of\n" +
                "an: Committee could best be reached\n" +
                "stress and challenge.\n" +
                "\n" +
                "ufa\n" +
                "\n" +
                "An\n" +
                "\n" +
                "It is recognized that outstanding\n" +
                "\n" +
                "1954\n" +
                "\n" +
                "es in. by concentrating, the investiga-\n" +
                "\n" +
                "sunted\n" +
                "\n" +
                "work has been done and is being\n" +
                "\n" +
                "only\n" +
                "cigarettes compared with 423 bil-\n" +
                "Aillion tions in the following three areas:\n" +
                "done here and abroad in these\n" +
                "\n" +
                "lion in 1953.\n" +
                "\n" +
                "The physical and chemical com-\n" +
                "ftelds :: The Scientitic Advisory\n" +
                "\n" +
                "position of tobacco and\n" +
                "accom-\n" +
                "Board plans to avoid repeating\n" +
                "\n" +
                "01138186A\n" +
                "\n" +
                "RESEARCH COMMITTEE\n" +
                "\n" +
                "panying products, buch aa\n" +
                "cig-\n" +
                "work that: has produced scientifi-\n" +
                "The Tobacco Industry Research arette papers and additives. This cally acceptable results. Where re\n" +
                "Committee was organized in Jan- covers the preparation, fraction- sults have been. inconclusive or\n" +
                "uary 1954 for the purpose of spon- ation and analysis ot tobacco and questionable\n" +
                "from a.\n" +
                "scfenti-\n" +
                "\n" +
                "soring independent research into of added substances.\n" +
                "\n" +
                "fic: standpoint, however, .further\n" +
                "\n" +
                "tobacco use and health.\n" +
                "\n" +
                "Tissue changes in humans as research will be: initlated.\n" +
                "The Committee was fortunate in well as in animals, in normal life\n" +
                "\n" +
                "The total annual budget of the\n" +
                "being able to get a group of emi- or under laboratory conditions, Tobaeno Industry Research Com-\n" +
                "nent scientists whose competence subjected to various types, dura- mittee ia slightly in excess of one\n" +
                "is securely established in their re. tion and! intensity of expoaure to million dollars. Research grants\n" +
                "spective fields: to serve as mem. various tobaccos and derivatives, have already been made to mev-\n" +
                "eral institutions totaling mere\n" +
                "than $300.000 ..";

        double similarity = computeSimilarity(result, expected);
        log.debug("OCR similarity score: {}%", similarity * 100);
        log.debug(result);
        log.debug(expected);


        assertTrue(similarity >= 0.70,
                String.format("Expected at least 80%% similarity but got %.2f%%", similarity * 100));
    }

    @Test
    @DisplayName("RapidOCR - ocrimg7.png should recognize quote beginning with 'If the path be beautiful'")
    void testRapidOCR_Image7_WithExpectedText() throws Exception {
        File file = new File(getClass().getClassLoader().getResource("attachments/ocrimg7.png").getFile());

        String result = OCRUtil.imageRapidOCR(file);

        assertNotNull(result, "RapidOCR result must not be null");
        assertTrue(result.contains("If the path be beautiful"));
        log.debug("RapidOCR test.png result: {}", result);
    }

    @Test
    @DisplayName("RapidOCR - ocrimg8.jpg should recognize quote beginning with 'Everyone has three lives'")
    void testRapidOCR_Image8_WithExpectedText() throws Exception {
        File file = new File(getClass().getClassLoader().getResource("attachments/ocrimg8.jpg").getFile());

        String result = OCRUtil.imageRapidOCR(file);
        log.debug("RapidOCR 10_test.jpg result: {}", result);

        assertNotNull(result, "RapidOCR result must not be null");
        assertTrue(result.contains("Everyone has threelives:"));
    }

    /**
     * Reads the content of a file and returns it as a String
     */
    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine()).append("\n");
            }
        }
        return content.toString().trim();
    }

    /**
     * Computes similarity between two strings using Levenshtein distance.
     * Returns a value between 0.0 (no match) and 1.0 (exact match).
     */
    private double computeSimilarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        if (a.equals(b)) return 1.0;

        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;

        return 1.0 - (double) levenshteinDistance(a, b) / maxLen;
    }

    private int levenshteinDistance(String a, String b) {
        int[] dp = new int[b.length() + 1];

        for (int i = 0; i <= b.length(); i++) dp[i] = i;

        for (int i = 1; i <= a.length(); i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int temp = dp[j];
                dp[j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? prev
                        : 1 + Math.min(prev, Math.min(dp[j], dp[j - 1]));
                prev = temp;
            }
        }
        return dp[b.length()];
    }
}