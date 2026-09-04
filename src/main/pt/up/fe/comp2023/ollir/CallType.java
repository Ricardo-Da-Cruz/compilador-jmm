package pt.up.fe.comp2023.ollir;

public enum CallType {
    invokevirtual("invokevirtual"),
    invokeinterface("invokeinterface"),
    invokespecial("invokespecial"),
    invokestatic("invokestatic"),
    NEW("NEW"),
    arraylength("arraylength"),
    ldc("ldc");

    public final String label;

    CallType(String s) {
        this.label = s;
    }
}
