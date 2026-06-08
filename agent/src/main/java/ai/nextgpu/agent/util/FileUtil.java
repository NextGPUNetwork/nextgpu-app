package ai.nextgpu.agent.util;

import ai.nextgpu.common.model.FileType;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.xml.sax.ContentHandler;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.cos.COSName;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FileUtil {

    /**
     * Extracts text content from a file by detecting its type and dispatching
     * to the appropriate parser. Supports PDF, DOCX, DOC, PPTX, XLSX, XML,
     * JSON, MD, TXT, and MP3 (metadata only). Falls back to Tika's
     * {@link AutoDetectParser} for unrecognized file types.
     *
     * @param file the file to extract content from
     * @return the extracted text content, or audio metadata formatted as
     *         key-value pairs for MP3 files
     * @throws Exception if the file type cannot be determined or extraction fails
     */
    public static String extractFile(File file) throws Exception {
        String fileName = file.getName();
        FileType fileType = FileType.fromFileName(fileName);

        try {
            if (fileType == null) {
                return extractWithAutoParser(file);
            }

            return switch (fileType) {
                case PDF -> FileUtil.extractPdfSmart(file);
                case DOCX -> FileUtil.extractDocx(file);
                case DOC -> FileUtil.extractDoc(file);
                case PPTX -> FileUtil.extractPptx(file);
                case XLSX -> FileUtil.extractXlsx(file);
                case XML, JSON, MD, TXT -> FileUtil.fileToString(file);
                case MP3 -> {
                    Metadata metadata = FileUtil.extractAudioMetadata(file);
                    StringBuilder sb = new StringBuilder();
                    for (String name : metadata.names()) {
                        sb.append(name).append(": ").append(metadata.get(name)).append("\n");
                    }
                    yield sb.toString();
                }
                default -> extractWithAutoParser(file);
            };
        } catch (Exception e) {
            throw new Exception("Failed to extract file: " + fileName, e);
        }
    }

    /**
     * Intelligently extracts text from a PDF by combining native text extraction
     * with OCR when necessary. If the PDF contains no images and has sufficient
     * native text, only the native extraction result is returned. If images are
     * present, native extraction and OCR are run concurrently and their results
     * are merged to maximize coverage (e.g. for scanned or mixed-content PDFs).
     *
     * @param file the PDF file to extract text from
     * @return the extracted text; a concatenation of native and OCR results
     *         if images are detected, or native text alone otherwise
     * @throws Exception if loading, rendering, or parsing the PDF fails
     */
    public static String extractPdfSmart(File file) throws Exception {
        String nativeText = FileUtil.extractPdf(file);

        if (hasPdfUsableText(nativeText) && !hasImages(file)) {
            return nativeText;
        }

        if (!hasImages(file)) {
            return nativeText;
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<String> nativeFuture = executor.submit(() -> FileUtil.extractPdf(file));
        Future<String> ocrFuture    = executor.submit(() -> FileUtil.extractPdfViaOCR(file));

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        String ocr    = ocrFuture.get();
        String native_ = nativeFuture.get();

        if (native_ == null) native_ = "";
        if (ocr == null)     ocr     = "";

        return (native_ + "\n" + ocr).trim();
    }

    /**
     * Checks whether any page in the PDF contains at least one image XObject
     * in its resources. Used to decide whether OCR should be attempted.
     *
     * @param file the PDF file to inspect
     * @return {@code true} if at least one image XObject is found; {@code false} otherwise
     * @throws Exception if the PDF cannot be loaded or its resources cannot be read
     */
    private static boolean hasImages(File file) throws Exception {
        try (PDDocument doc = PDDocument.load(file)) {
            for (PDPage page : doc.getPages()) {
                PDResources resources = page.getResources();
                if (resources == null) continue;
                for (COSName name : resources.getXObjectNames()) {
                    if (resources.isImageXObject(name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Extracts text from a PDF by rendering each page to a 300 DPI PNG image
     * and running OCR on it via {@link OCRUtil#imageRapidOCR}. Each page's
     * result is prefixed with a page number header. Temporary image files are
     * deleted after processing regardless of success or failure.
     *
     * @param file the PDF file to OCR
     * @return the OCR'd text for all pages, separated by blank lines; an empty
     *         string if no text was recognized on any page
     * @throws Exception if the PDF cannot be loaded, a page cannot be rendered,
     *                   or a temporary file cannot be created
     */
    public static String extractPdfViaOCR(File file) throws Exception {
        List<String> pageTexts = new ArrayList<>();

        try (PDDocument document = PDDocument.load(file)) {
            PDFRenderer renderer = new PDFRenderer(document);

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(i, 300, ImageType.RGB);
                File tempImage = File.createTempFile("pdf_page_" + i + "_", ".png");

                try {
                    ImageIO.write(pageImage, "PNG", tempImage);
                    String pageText = OCRUtil.imageRapidOCR(tempImage);
                    if (pageText != null && !pageText.isBlank()) {
                        pageTexts.add("-- Page " + (i + 1) + " --\n" + pageText.strip());
                    }
                } finally {
                    tempImage.delete();
                }
            }
        }

        return String.join("\n\n", pageTexts);
    }

    /**
     * Determines whether the extracted PDF text contains enough content to be
     * considered usable, defined as at least 50 non-whitespace characters.
     *
     * @param text the text extracted from a PDF
     * @return {@code true} if the text has 50 or more non-whitespace characters;
     *         {@code false} if the text is null, blank, or too short
     */
    private static boolean hasPdfUsableText(String text) {
        if (text == null || text.isBlank()) return false;
        return text.replaceAll("\\s+", "").length() >= 50;
    }

    /**
     * Extracts text from a file using Tika's {@link AutoDetectParser}, which
     * automatically identifies the MIME type and selects an appropriate parser.
     * Embedded documents (e.g. attachments within a container format) are
     * parsed recursively; any malformed embedded content is silently skipped.
     *
     * @param file the file to parse
     * @return the full extracted text content
     * @throws Exception if the file cannot be opened or parsing fails
     */
    public static String extractWithAutoParser(File file) throws Exception {
        try (InputStream inputStream = new FileInputStream(file)) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            context.set(EmbeddedDocumentExtractor.class, new ParsingEmbeddedDocumentExtractor(context) {
                @Override
                public void parseEmbedded(InputStream stream, ContentHandler handler,
                                          Metadata embeddedMetadata, boolean outputHtml) {
                    try {
                        super.parseEmbedded(stream, handler, embeddedMetadata, outputHtml);
                    } catch (Exception | AssertionError e) {
                        // Skip malformed embedded content
                    }
                }
            });

            Parser parser = new AutoDetectParser();
            parser.parse(inputStream, handler, metadata, context);
            return handler.toString();
        }
    }

    /**
     * Extracts text content from a DOCX file using Tika's {@link OOXMLParser}.
     *
     * @param file the DOCX file to parse
     * @return the extracted text content
     * @throws Exception if the file cannot be opened or parsing fails
     */
    public static String extractDocx(File file) throws Exception {
        try (InputStream inputStream = new FileInputStream(file)) {
            WriteOutContentHandler handler = new WriteOutContentHandler(-1);
            Metadata metadata = new Metadata();
            OOXMLParser parser = new OOXMLParser();
            ParseContext context = new ParseContext();
            parser.parse(inputStream, handler, metadata, context);
            return handler.toString();
        }
    }

    /**
     * Extracts text content from a legacy DOC file using Tika's {@link OfficeParser}.
     *
     * @param file the DOC file to parse
     * @return the extracted text content
     * @throws Exception if the file cannot be opened or parsing fails
     */
    public static String extractDoc(File file) throws Exception {
        try (InputStream inputStream = new FileInputStream(file)) {
            WriteOutContentHandler handler = new WriteOutContentHandler(-1);
            Metadata metadata = new Metadata();
            OfficeParser parser = new OfficeParser();
            ParseContext context = new ParseContext();
            parser.parse(inputStream, handler, metadata, context);
            return handler.toString();
        }
    }

    /**
     * Extracts text content from a PDF file using Tika's {@link PDFParser}.
     * This performs native text extraction only and does not invoke OCR.
     * For a smarter extraction strategy that includes OCR fallback, use
     * {@link #extractPdfSmart(File)} instead.
     *
     * @param file the PDF file to parse
     * @return the extracted text content
     * @throws Exception if the file cannot be opened or parsing fails
     */
    public static String extractPdf(File file) throws Exception {
        try (InputStream inputStream = new FileInputStream(file)) {
            WriteOutContentHandler handler = new WriteOutContentHandler(-1);
            Metadata metadata = new Metadata();
            PDFParser parser = new PDFParser();
            ParseContext context = new ParseContext();
            parser.parse(inputStream, handler, metadata, context);
            return handler.toString();
        }
    }

    /**
     * Extracts text content from a PPTX file using Tika's {@link OOXMLParser}.
     *
     * @param file the PPTX file to parse
     * @return the extracted text content from all slides
     * @throws Exception if the file cannot be opened or parsing fails
     */
    public static String extractPptx(File file) throws Exception {
        try (InputStream inputStream = new FileInputStream(file)) {
            WriteOutContentHandler handler = new WriteOutContentHandler(-1);
            Metadata metadata = new Metadata();
            OOXMLParser parser = new OOXMLParser();
            ParseContext context = new ParseContext();
            parser.parse(inputStream, handler, metadata, context);
            return handler.toString();
        }
    }

    /**
     * Extracts text content from an XLSX file using Tika's {@link OOXMLParser}.
     *
     * @param file the XLSX file to parse
     * @return the extracted text content from all sheets
     * @throws Exception if the file cannot be opened or parsing fails
     */
    public static String extractXlsx(File file) throws Exception {
        try (InputStream inputStream = new FileInputStream(file)) {
            WriteOutContentHandler handler = new WriteOutContentHandler(-1);
            Metadata metadata = new Metadata();
            OOXMLParser parser = new OOXMLParser();
            ParseContext context = new ParseContext();
            parser.parse(inputStream, handler, metadata, context);
            return handler.toString();
        }
    }

    /**
     * Extracts metadata (e.g. artist, album, title, duration) from an MP3 file
     * using Tika's {@link Mp3Parser}. The returned {@link Metadata} object can
     * be iterated over its {@code names()} to retrieve all available tag fields.
     *
     * @param file the MP3 file to read metadata from
     * @return a {@link Metadata} instance populated with the audio file's tags
     * @throws Exception if the file cannot be opened or metadata extraction fails
     */
    public static Metadata extractAudioMetadata(File file) throws Exception {
        try (InputStream inputStream = new FileInputStream(file)) {
            Metadata metadata = new Metadata();
            Mp3Parser parser = new Mp3Parser();
            ParseContext context = new ParseContext();
            parser.parse(inputStream, new WriteOutContentHandler(), metadata, context);
            return metadata;
        }
    }

    /**
     * Reads the entire contents of a plain-text file into a {@link String}
     * using the platform's default charset. Suitable for XML, JSON, Markdown,
     * and TXT files where no parsing or extraction logic is needed.
     *
     * @param file the text file to read
     * @return the full file contents as a string
     * @throws Exception if the file cannot be read
     */
    public static String fileToString(File file) throws Exception {
        return java.nio.file.Files.readString(file.toPath());
    }
}