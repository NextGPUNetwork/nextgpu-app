package ai.nextgpu.common.model;

public enum StorageUnit {
    BIT("b"),
    BYTE("B"),
    KILOBYTE("KB"),
    MEGABYTE("MB"),
    GIGABYTE("GB"),
    TERABYTE("TB"),
    PETABYTE("PB"),
    EXABYTE("EB"),
    ZETTABYTE("ZB"),
    YOTTABYTE("YB");

    private final String abbreviation;

    StorageUnit(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
