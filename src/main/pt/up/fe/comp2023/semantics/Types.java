package pt.up.fe.comp2023.semantics;

public enum Types {
    INT("int"),
    BOOLEAN("boolean"),
    STRING("String"),
    CHAR("char"),
    ANY("any"),
    VOID("void"),
    NULL("null");

    private final String name;

    Types(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
