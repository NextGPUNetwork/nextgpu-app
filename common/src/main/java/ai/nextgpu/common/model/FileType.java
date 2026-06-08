package ai.nextgpu.common.model;

import lombok.Getter;

@Getter
public enum FileType {
    // Text and documents
    TXT(".txt"),
    MD(".md"),
    PDF(".pdf"),
    DOC(".doc"),
    DOCX(".docx"),
    ODT(".odt"),
    RTF(".rtf"),
    PPTX(".pptx"),

    // Data and configs
    JSON(".json"),
    XLSX(".xlsx"),
    YAML(".yaml"),
    YML(".yml"),
    XML(".xml"),
    CSV(".csv"),
    TSV(".tsv"),
    INI(".ini"),
    ENV(".env"),

    // Code and scripts
    JAVA(".java"),
    KT(".kt"),
    PY(".py"),
    JS(".js"),
    TS(".ts"),
    GO(".go"),
    RS(".rs"),
    CPP(".cpp"),
    C(".c"),
    H(".h"),
    CS(".cs"),
    PHP(".php"),
    SH(".sh"),
    BAT(".bat"),

    // Web
    HTML(".html"),
    CSS(".css"),
    SVG(".svg"),

    // Images
    PNG(".png"),
    JPG(".jpg"),
    JPEG(".jpeg"),
    WEBP(".webp"),
    GIF(".gif"),

    // Audio
    MP3(".mp3"),
    WAV(".wav"),

    // Video
    MP4(".mp4"),
    AVI(".avi"),
    MOV(".mov"),
    MKV(".mkv"),
    FLV(".flv"),
    WMV(".wmv"),
    WEBM(".webm"),

    // Archives
    ZIP(".zip"),
    TAR(".tar"),
    GZ(".gz"),
    RAR(".rar"),
    SEVEN_Z(".7z"),

    // Databases and data dumps
    SQL(".sql"),
    DB(".db"),
    SQLITE(".sqlite"),

    // Logs and misc
    LOG(".log"),
    CONF(".conf");

    private final String extension;

    FileType(String extension) {
        this.extension = extension;
    }

    public static FileType fromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String lowerFileName = fileName.toLowerCase();
        for (FileType type : FileType.values()) {
            if (lowerFileName.endsWith(type.getExtension())) {
                return type;
            }
        }
        return null;
    }
}
