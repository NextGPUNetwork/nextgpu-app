package ai.nextgpu.agent.util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.benjaminwan.ocrlibrary.OcrResult;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import java.io.File;


public class OCRUtil {
    private static final Logger log = LoggerFactory.getLogger(OCRUtil.class);
    /**
     * Performs OCR on the given image file using the RapidOCR inference engine
     * with the ONNX PPOCRv4 model, and returns the recognized text as a string.
     *
     * @param file the image file to perform OCR on; must be non-null and exist on disk
     * @return the recognized text extracted from the image, or {@code null} if the
     *         file is null or does not exist
     * @throws Exception if the OCR engine fails to initialize or process the image
     */
    public static String imageRapidOCR(File file) throws Exception {
        if (file == null || !file.exists()) {
            log.error("Image file not found!");
            return null;
        }

        String imagePath = file.getAbsolutePath();
        log.info("Using image path: {}", imagePath);

        InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);
        OcrResult result = engine.runOcr(imagePath);

        return result.getStrRes();
    }

}