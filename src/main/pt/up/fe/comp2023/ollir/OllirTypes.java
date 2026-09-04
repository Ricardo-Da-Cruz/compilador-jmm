package pt.up.fe.comp2023.ollir;

public enum OllirTypes {
    INT(".i32"),
    BOOLEAN(".bool"),
    STRING(".String"),
    VOID(".V"),
    ARRAY(".array");

    private final String label;
    OllirTypes(String s) {
        this.label = s;
    }

    public String getLabel() {
        return label;
    }
}
