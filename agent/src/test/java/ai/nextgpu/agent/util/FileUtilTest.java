package ai.nextgpu.agent.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ai.nextgpu.agent.service.BaseTest;
import org.apache.tika.metadata.Metadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FileUtilTest extends BaseTest {
    private static final Logger log = LoggerFactory.getLogger(FileUtilTest.class);

    @TempDir
    Path tempDir;

    @Test
    void testFileToString_withTextFile() throws Exception {
        // Create a temporary text file with known content
        String testContent = "Hello, World!\nThis is a test file.";
        File textFile = tempDir.resolve("test.txt").toFile();

        try (FileWriter writer = new FileWriter(textFile)) {
            writer.write(testContent);
        }

        // Test the fileToString method
        String result = FileUtil.fileToString(textFile);
        assertEquals(testContent, result, "File content should match");
    }

    @Test
    void testFileToString_withEmptyFile() throws Exception {
        // Create an empty temporary file
        File emptyFile = tempDir.resolve("empty.txt").toFile();
        emptyFile.createNewFile();

        // Test the fileToString method
        String result = FileUtil.fileToString(emptyFile);
        assertEquals("", result, "Empty file should return empty string");
    }

    @Test
    void testFileToString_withNonExistentFile() {
        // Test with a non-existent file
        File nonExistentFile = tempDir.resolve("nonexistent.txt").toFile();

        assertThrows(Exception.class, () -> {
            FileUtil.fileToString(nonExistentFile);
        }, "Should throw exception for non-existent file");
    }

    @Test
    void testExtractPdf_withValidPdf() throws Exception {

        File pdfFile = new File(getClass().getClassLoader().getResource("attachments/test.pdf").getFile());

        // Test the extractPdf method
        String result = FileUtil.extractPdf(pdfFile);
        assertNotNull(result, "Result should not be null");
        assertTrue(result.contains("International Journal for Research Trends and Innovation"), "Result should contain given text");
    }

    @Test
    void testExtractPdf_withInvalidPdf() {

        File invalidPdfFile = new File(
                getClass().getClassLoader()
                        .getResource("attachments/test.json")
                        .getFile());

        assertThrows(Exception.class, () -> {
            FileUtil.extractPdf(invalidPdfFile);
        });
    }

    @Test
    void testExtractDocx_withValidDocx() throws Exception {
        File docxFile = new File(getClass().getClassLoader().getResource("attachments/test.docx").getFile());

        // Test the extractDocx method
        String result = FileUtil.extractDocx(docxFile);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.length() > 0, "DOCX should have extracted content");
        assertTrue(result.contains("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc ac faucibus odio"));
    }

    @Test
    void testExtractDocx_withInvalidDocx() {
        File invalidDocxFile = new File(
                getClass().getClassLoader()
                        .getResource("attachments/test.json")
                        .getFile());

        assertThrows(Exception.class, () -> {
            FileUtil.extractDocx(invalidDocxFile);
        });
    }

    @Test
    void testExtractDoc_withValidDoc() throws Exception {
        File docFile = new File(getClass().getClassLoader().getResource("attachments/test.doc").getFile());

        // Test the extractDoc method
        String result = FileUtil.extractDoc(docFile);
        assertNotNull(result, "Result should not be null");
        assertTrue(result.length() > 0, "DOC should have extracted content");
        assertTrue(result.contains("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc ac faucibus odio. "),
                "DOC should contain actual text content");
    }

    @Test
    void testExtractDoc_withInvalidDoc() {
        File invalidDocFile = new File(
                getClass().getClassLoader()
                        .getResource("attachments/test.json")
                        .getFile());

        assertThrows(Exception.class, () -> {
            FileUtil.extractDoc(invalidDocFile);
        });
    }

    @Test
    void testExtractPptx_withValidPptx() throws Exception {
        File pptxFile = new File(getClass().getClassLoader().getResource("attachments/test.pptx").getFile());

        // Test the extractPptx method
        String result = FileUtil.extractPptx(pptxFile);
        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("Discoverability and Analytics"));
    }

    @Test
    void testExtractPptx_withInvalidPptx() {
        File invalidPptxFile = new File(
                getClass().getClassLoader()
                        .getResource("attachments/test.json")
                        .getFile());

        assertThrows(Exception.class, () -> {
            FileUtil.extractPptx(invalidPptxFile);
        });
    }

    @Test
    void testExtractXlsx_withValidXlsx() throws Exception {
        File xlsxFile = new File(getClass().getClassLoader().getResource("attachments/test.xlsx").getFile());

        // Test the extractXlsx method
        String result = FileUtil.extractXlsx(xlsxFile);
        assertNotNull(result, "Result should not be null");
        assertTrue(result.length() > 0, "XLSX should have extracted content");
        assertTrue(result.contains("Google Finance"));
        assertTrue(result.contains("Market Cap"));
    }

    @Test
    void testExtractXlsx_withNonExistentFile() {
        // Test with a non-existent file
        File nonExistentFile = tempDir.resolve("nonexistent.xlsx").toFile();

        assertThrows(Exception.class, () -> {
            FileUtil.extractXlsx(nonExistentFile);
        }, "Should throw exception for non-existent file");
    }

    @Test
    void testExtractXlsx_withInvalidXlsx() {
        File invalidXlsxFile = new File(
                getClass().getClassLoader()
                        .getResource("attachments/test.json")
                        .getFile()
        );

        assertThrows(Exception.class, () -> {
            FileUtil.extractXlsx(invalidXlsxFile);
        });
    }

    @Test
    void testExtractAudioMetadata_withValidMp3() throws Exception {

        File mp3File = new File(
                getClass().getClassLoader()
                        .getResource("attachments/test.mp3")
                        .getFile()
        );

        assertTrue(mp3File.exists(), "Test MP3 file should exist");
        Metadata metadata = FileUtil.extractAudioMetadata(mp3File);

        assertNotNull(metadata, "Metadata should not be null");
        assertTrue(metadata.names().length > 0,
                "Metadata should contain at least one field");

        String title = metadata.get("dc:title");
        assertNotNull(title, "Title metadata should exist");
        assertEquals("King Audio", title,
                "Title should match expected value");

        String contentType = metadata.get("Content-Type");
        assertNotNull(contentType, "Content-Type should exist");
        assertEquals("audio/mpeg", contentType,
                "Content type should be audio/mpeg");
    }

    @Test
    void testFileToString_withValidXml() throws Exception {
        File xmlFile = new File(getClass().getClassLoader().getResource("attachments/test.xml").getFile());

        // Test the fileToString method
        String result = FileUtil.fileToString(xmlFile);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.contains("Toyota"));
        assertTrue(result.contains("Honda"));
        assertTrue(result.contains("Ford"));
    }


    @Test
    void testFileToString_withValidJson() throws Exception {
        File jsonFile = new File(getClass().getClassLoader().getResource("attachments/test.json").getFile());

        // Test the fileToString method
        String result = FileUtil.fileToString(jsonFile);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.contains("Ford"));
        assertTrue(result.contains("Honda"));
    }

    @Test
    void testFileToString_withSpecialCharacters() throws Exception {
        // Create a file with special characters
        String specialContent = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?\nUnicode: 你好世界 🎉";
        File specialFile = tempDir.resolve("special.txt").toFile();

        try (FileWriter writer = new FileWriter(specialFile)) {
            writer.write(specialContent);
        }

        // Test the fileToString method
        String result = FileUtil.fileToString(specialFile);
        assertEquals(specialContent, result, "Special characters should be preserved");
    }


    @Test
    void testFileToString_withValidMarkdown() throws Exception {
        File mdFile = new File(
                getClass().getClassLoader()
                        .getResource("attachments/test.md")
                        .getFile()
        );

        String result = FileUtil.fileToString(mdFile);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.length() > 0, "Markdown file should have content");
        assertTrue(result.contains("Tesla"));
        assertTrue( result.contains("Adaptive Cruise Control"));
    }

    @Test
    void testExtractFile_withImgPdf() throws Exception {
        File pdfFile = new File(getClass().getClassLoader().getResource("attachments/img.pdf").getFile());
        String result = FileUtil.extractFile(pdfFile);
        log.debug(result);

        assertNotNull(result, "Result should not be null for PDF");
        assertTrue(result.contains("THIS IS ADDED TEXT NOT A IMAGE"));
        assertTrue(result.contains("Thank you very much for the privilege of reading this manuscript."));
    }

    @Test
    void testExtractFile_withPdf() throws Exception {
        File pdfFile = new File(getClass().getClassLoader().getResource("attachments/test.pdf").getFile());
        String result = FileUtil.extractFile(pdfFile);

        assertNotNull(result, "Result should not be null for PDF");
        assertTrue(result.contains("International Journal for Research Trends and Innovation"));
    }

    @Test
    void testExtractFile_withDocx() throws Exception {
        File docxFile = new File(getClass().getClassLoader().getResource("attachments/test.docx").getFile());
        String result = FileUtil.extractFile(docxFile);

        assertNotNull(result, "Result should not be null for DOCX");
        assertTrue(result.contains("Maecenas non lorem quis tellus placerat varius. "), "DOCX extraction should return content");
    }

    @Test
    void testExtractFile_withDoc() throws Exception {
        File docFile = new File(getClass().getClassLoader().getResource("attachments/test.doc").getFile());
        String result = FileUtil.extractFile(docFile);

        assertNotNull(result, "Result should not be null for DOC");
        assertTrue(result.contains("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc ac faucibus odio. "),
                "DOC should contain actual text content");
    }

    @Test
    void testExtractFile_withPptx() throws Exception {
        File pptxFile = new File(getClass().getClassLoader().getResource("attachments/test.pptx").getFile());
        String result = FileUtil.extractFile(pptxFile);

        assertNotNull(result, "Result should not be null for PPTX");
        assertFalse(result.isBlank());
        assertTrue(result.contains("Discoverability and Analytics"));
    }

    @Test
    void testExtractFile_withPpt() throws Exception {
        File pptFile = new File(getClass().getClassLoader().getResource("attachments/test.ppt").getFile());
        String result = FileUtil.extractFile(pptFile);
        log.debug(result);

        assertNotNull(result, "Result should not be null for PPT");
        assertFalse(result.isBlank());
        assertTrue(result.contains("Lorem ipsum dolor sit amet"));
    }


    @Test
    void testExtractFile_withXlsx() throws Exception {
        File xlsxFile = new File(getClass().getClassLoader().getResource("attachments/test.xlsx").getFile());

        String result = FileUtil.extractFile(xlsxFile);

        assertNotNull(result, "Result should not be null for XLSX");
        assertTrue(result.length() > 0, "XLSX extraction should return content");
        assertTrue(result.contains("Google Finance"));
        assertTrue(result.contains("Market Cap"));

    }

    @Test
    void testExtractFile_withMp3() throws Exception {
        File mp3File = new File(getClass().getClassLoader().getResource("attachments/test.mp3").getFile());

        String result = FileUtil.extractFile(mp3File);

        assertNotNull(result, "Result should not be null for MP3");
        assertTrue(result.contains("King Audio"));
        assertTrue(result.contains("Testing"));
    }

    @Test
    void testExtractFile_withXml() throws Exception {
        File xmlFile = new File(getClass().getClassLoader().getResource("attachments/test.xml").getFile());

        String result = FileUtil.extractFile(xmlFile);

        assertNotNull(result, "Result should not be null for XML");
        assertTrue(result.contains("Toyota"));
        assertTrue(result.contains("Honda"));
        assertTrue(result.contains("Ford"));
    }

    @Test
    void testExtractFile_withTex() throws Exception {
        File texFile = new File(getClass().getClassLoader().getResource("attachments/test.xml").getFile());

        String result = FileUtil.extractFile(texFile);
        log.debug(result);

        assertNotNull(result, "Result should not be null for TEX");
        assertTrue(result.contains("<feature>Lane Departure Warning</feature>"));
    }

    @Test
    void testExtractFile_withJson() throws Exception {
        File jsonFile = new File(getClass().getClassLoader().getResource("attachments/test.json").getFile());

        String result = FileUtil.extractFile(jsonFile);

        assertNotNull(result, "Result should not be null for JSON");
        assertTrue(result.contains("Ford"));
        assertTrue(result.contains("Honda"));
    }

    @Test
    void testExtractFile_withMarkdown() throws Exception {
        File mdFile = new File(getClass().getClassLoader().getResource("attachments/test.md").getFile());

        String result = FileUtil.extractFile(mdFile);

        assertNotNull(result, "Result should not be null for Markdown");
        assertTrue(result.contains("Dual Motor"));
        assertTrue(result.contains("Tesla"));
    }

    @Test
    void testExtractFile_withTxt() throws Exception {
        // Create a temporary text file
        String testContent = "This is a test text file.";
        File txtFile = tempDir.resolve("test.txt").toFile();

        try (FileWriter writer = new FileWriter(txtFile)) {
            writer.write(testContent);
        }

        String result = FileUtil.extractFile(txtFile);
        assertEquals(testContent, result, "TXT content should match");
    }
}
