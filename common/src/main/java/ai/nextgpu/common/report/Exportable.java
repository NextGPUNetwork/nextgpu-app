package ai.nextgpu.common.report;

/**
 * Makes implementation entity exportable to a file like PDF or HTML
 */
public interface Exportable {

    /**
     * Exports this entity as a styled HTML file.
     *
     * @param filename path of the HTML file to write (e.g. "report.html")
     */
    void exportToHtml(String filename);

    /**
     * Exports this entity as a PDF file.
     *
     * @param filename path of the PDF file to write (e.g. "report.pdf")
     */
    void exportToPdf(String filename);

    /**
     * Exports this entity as a plain text file.
     *
     * @param filename path of the text file to write (e.g. "report.txt")
     */
    void exportToText(String filename);
}
