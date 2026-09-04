package pt.up.fe.comp2023.ollir;

public enum Operators {
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/"),
    EQ("=="),
    NEQ("!="),
    LT("<"),
    AND("&&"),
    OR("||"),;

    private final String label;

    Operators(String s) {
        this.label = s;
    }

    public String getLabel() {
        return label;
    }


}
