package ai.nextgpu.common.report;

/**
 * Makes implementation entity exportable to a file like PDF or HTML
 */
public interface Exportable {

    void exportToHtml(String filename);

    void exportToPdf(String filename);

    void exportToText(String filename);
}
