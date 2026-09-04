package pt.up.fe.comp2023.ollir;

public enum NonAcessModifiers {
    FINAL("final"),
    STATIC("static"),
    ABSTRACT("abstract"),
    NONE("");

    private final String label;

    NonAcessModifiers(String s) {
        this.label = s;
    }


    public String getLabel() {
        return label;
    }
}
