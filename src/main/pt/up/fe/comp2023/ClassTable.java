package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;

public class ClassTable implements SymbolTable {
    private final List<String[]> imports = new ArrayList<>();
    private final List<Symbol> fields = new ArrayList<>();
    private final List<MethodTable> methods = new ArrayList<>();
    private String className;



    private String superName;


    public ClassTable(JmmNode root) {
        assert root.getKind().equals("Program")
                : "Expected root node to be of kind 'Program', got '" + root.getKind() + "'";
        var children = root.getChildren();
        for (JmmNode child : children) {
            if (child.getKind().equals("ImportDeclaration")){
                String[] temp = child.get("values")
                        .substring(1,child.get("values").length()-1).
                        split(",");
                for (int i = 0; i < temp.length; i++) {
                    temp[i] = temp[i].trim();
                }
                imports.add(temp);
            } else if (child.getKind().equals("ClassDeclaration")) {
                    processClassDeclaration(child);
                break;
            }
        }
    }
    private void processClassDeclaration(JmmNode node){
        className = node.get("name");
        if (node.hasAttribute("superClass"))
            superName = node.get("superClass");
        var children = node.getChildren();
        for (JmmNode child : children) {
            if (child.getKind().equals("VariableDeclaration") || child.getKind().equals("Initialization")) {
                processFieldDeclaration(child);
            } else if (child.getKind().equals("Method")) {
                methods.add(new MethodTable(child));
            }
        }
    }
    private void processFieldDeclaration(JmmNode node) {
        String name = node.get("name");
        if (node.getJmmChild(0).getKind().equals("ArrayType")){
            if (node.getJmmChild(0).getJmmChild(0).getKind().equals("ArrayType")){
                throw new RuntimeException("Arrays of arrays are not supported");
            }
            Type type = new Type(node.getJmmChild(0).getJmmChild(0).get("value"), true);
            this.fields.add(new Symbol(type, name));
        } else {
            Type type = new Type(node.getJmmChild(0).get("value"), false);
            this.fields.add(new Symbol(type, name));
        }
    }

    @Override
    public List<String> getImports() {
        // turn the list of string arrays into a list of strings
        List<String> importsList = new ArrayList<>();
        for (String[] importArray : imports) {
            importsList.add(String.join(".", importArray));
        }
        return importsList;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superName;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    public Type getFieldType(String name) {
        for (Symbol field : fields) {
            if (field.getName().equals(name))
                return field.getType();
        }
        return null;
    }



    @Override
    public List<String> getMethods() {
        List<String> methodsList = new ArrayList<>();
        for (MethodTable method : methods) {
            methodsList.add(method.getMethodName());
        }
        return methodsList;
    }

    @Override
    public Type getReturnType(String methodName) {
        for (MethodTable method : methods) {
            if (method.getMethodName().equals(methodName))
                return method.getReturnType();
        }
        return null;
    }

    public Boolean isStatic(String methodName) {
        for (MethodTable method : methods) {
            if (method.getMethodName().equals(methodName))
                return method.isStatic();
        }
        return null;
    }

    public Boolean isPublic(String methodName) {
        for (MethodTable method : methods) {
            if (method.getMethodName().equals(methodName))
                return method.isPublic();
        }
        return null;
    }

    public Boolean isMethod(String methodName) {
        for (MethodTable method : methods) {
            if (method.getMethodName().equals(methodName))
                return true;
        }
        return false;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        for (MethodTable method : methods) {
            if (method.getMethodName().equals(methodName))
                return method.getParameters();
        }
        return null;
    }

    public Boolean hasSuper() {
        return superName != null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        for (MethodTable method : methods) {
            if (method.getMethodName().equals(methodName))
                return method.getLocalVariables();
        }
        return null;
    }

    public Type getLocalVariableType(String methodName, String varName) {
        for (MethodTable method : methods) {
            if (method.getMethodName().equals(methodName))
                return method.getLocalVariable(varName);
        }
        return null;
    }

    public Type getParameterType(String methodName, String varName) {
        for (MethodTable method : methods) {
            if (method.getMethodName().equals(methodName))
                return method.getParameter(varName);
        }
        return null;
    }

    public Type getMethodType(String methodName) {
        for (MethodTable method : methods) {
            if (method.getMethodName().equals(methodName))
                return method.getReturnType();
        }
        return null;
    }


}
