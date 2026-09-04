package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;

public class MethodTable {
    String name;
    Type returnType;
    List<Symbol> parameters = new ArrayList<>();
    List<Symbol> localVariables = new ArrayList<>();
    String visibility;
    boolean isStatic = false;

    public MethodTable(JmmNode node) {
        assert node.getKind().equals("Method")
                : "Expected node to be of kind 'Method', got '" + node.getKind() + "'";
        System.out.println(node.getAttributes());
        if (node.hasAttribute("visibility"))
            this.visibility = node.get("visibility");
        if (node.hasAttribute("istatic"))
            this.isStatic = true;
        else this.visibility = "protected";
        this.name = node.get("name");

        // Process parameters
        if (node.getJmmChild(0).getKind().equals("ArrayType")){
            if (node.getJmmChild(0).getJmmChild(0).getKind().equals("ArrayType")){
                throw new RuntimeException("Array of arrays not supported");
            }
            this.returnType =  new Type(node.getJmmChild(0).getJmmChild(0).get("value"), true);
        } else {
            this.returnType = new Type(node.getJmmChild(0).get("value"), false);
        }

        // Process parameters
        var children = node.getChildren();
        int i = 1;
            while (children.size() > i && children.get(i).getKind().equals("VariableDeclaration")) {
                Type type;
                if (children.get(i).getJmmChild(0).getKind().equals("ArrayType")) {
                    if (children.get(i).getJmmChild(0).getJmmChild(0).getKind().equals("ArrayType")) {
                        throw new RuntimeException("Array of arrays not supported");
                    }
                    type = new Type(children.get(i).getJmmChild(0).getJmmChild(0).get("value"), true);
                    this.parameters.add(new Symbol(type, children.get(i).get("name")));
                } else {
                    type = new Type(children.get(i).getJmmChild(0).get("value"), false);
                    this.parameters.add(new Symbol(type, children.get(i).get("name")));
                }
                i++;
            }

        // Process local variables
        processLocalVariables(children, i);
    }

    public boolean isStatic() {
        return isStatic;
    }

    private void processLocalVariables(List<JmmNode> node, int i) {
        while(i < node.size()) {
            if (node.get(i).getKind().equals("DeclarationST")) {
                Type type;
                var declaration = node.get(i).getJmmChild(0);
                if (declaration.getJmmChild(0).getKind().equals("ArrayType")){
                    if (declaration.getJmmChild(0).getJmmChild(0).getKind().equals("ArrayType")){
                        throw new RuntimeException("Array of arrays not supported");
                    }
                    type = new Type(declaration.getJmmChild(0).getJmmChild(0).get("value"), true);
                    this.localVariables.add(new Symbol(type, declaration.get("name")));
                } else {
                    type = new Type(declaration.getJmmChild(0).get("value"), false);
                    this.localVariables.add(new Symbol(type, declaration.get("name")));
                }
            }else{
                processLocalVariables(node.get(i).getChildren(), 0);
            }
            i++;
        }

    }

    public String getMethodName() {
        return this.name;
    }

    public Type getReturnType() {
        return this.returnType;
    }

    public List<Symbol> getParameters() {
        return this.parameters;
    }

    public List<Symbol> getLocalVariables() {
        return this.localVariables;
    }

    public Type getLocalVariable( String name) {
        for (Symbol s : this.localVariables) {
            if (s.getName().equals(name)) {
                return s.getType();
            }
        }
        return null;
    }

    public Type getParameter(String varName) {
        for (Symbol s : this.parameters) {
            if (s.getName().equals(varName)) {
                return s.getType();
            }
        }
        return null;
    }

    public Boolean isPublic() {
        return this.visibility.equals("public");
    }
}
