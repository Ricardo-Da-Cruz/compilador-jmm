package pt.up.fe.comp2023.ollir;

public enum AccessModifiers {
    PUBLIC("public"),
    PRIVATE("private"),
    PROTECTED("protected"),
    DEFAULT("");

    private final String label;

    AccessModifiers(String s) {
        this.label = s;
    }

    public String getLabel() {
        return label;
    }

}
