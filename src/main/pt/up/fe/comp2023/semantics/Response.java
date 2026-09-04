package pt.up.fe.comp2023.semantics;


import pt.up.fe.comp.jmm.analysis.table.Type;

public class Response {
    private final Boolean result;
    private final Type type;

    public Response(Boolean result) {
        this.result = result;
        this.type = new Type("null",false);
    }

    public Response(Type type) {
        this.result = true;
        this.type = type;
    }

    public Boolean getResult() {
        return result;
    }

    public Type getType() {
        return type;
    }


}
