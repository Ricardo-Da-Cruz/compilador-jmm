package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.Map;
import java.util.Optional;

public class ASTConstantPropagation extends AJmmVisitor<Integer,Optional<String> > {

    SymbolTable symbolTable;

    Map<String, String> varValues;
    public ASTConstantPropagation(SymbolTable symbolTable) {
        super();
        varValues = new java.util.HashMap<>();
        this.symbolTable = symbolTable;
    }

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::defaultVisit);


        addVisit("IfElse",this::dealWithIf);


        addVisit("BinaryOp",this::dealWithBinaryOp);

        addVisit("Integer",(node,s) -> Optional.of(node.get("value")));
        addVisit("Boolean",(node,s) -> Optional.of(node.get("value")));
        addVisit( "Identifier", this::dealWithIdentifier);
        addVisit("Assignment", this::dealWithAssignment);
        //addVisit("Return", this::dealWithReturn);

    }
    /*
    private Optional<String> dealWithReturn(JmmNode node, Integer s) {
        Optional<String> value = visit(node.getChildren().get(0), 0);
        if (value.isPresent()){
            node.removeJmmChild(0);
            JmmNode child;
            if (isInteger(value.get()))
                child = new JmmNodeImpl("Integer");
            else
                child = new JmmNodeImpl("Boolean");
            child.put("value",value.get());
            node.getJmmParent().setChild(child,s);
        }
        return Optional.empty();
    }

     */

    private Optional<String> dealWithIf(JmmNode node, Integer s) {
        Optional<String> value = visit(node.getChildren().get(0), 0);
        if (value.isPresent()){
            if (value.get().equals("true")){
                System.out.println("fjaegfnwehst");
                node.getJmmParent().setChild(node.getChildren().get(1),s);
            }else if (node.getNumChildren() == 3){
                System.out.println("adbjfknwefa");
                node.getJmmParent().setChild(node.getChildren().get(2),s);
            }
        }
        return Optional.empty();
    }

    private Optional<String> dealWithAssignment(JmmNode node, Integer s) {
        Optional<String> value = visit(node.getChildren().get(0), 0);

        if (value.isPresent()){
            varValues.put(node.get("var"),value.get());
            node.getJmmParent().removeJmmChild(s);
        }else {
            varValues.remove(node.get("var"));
        }
        return Optional.empty();
    }

    private Optional<String> dealWithBinaryOp(JmmNode node, int s) {
        Optional<String> left = visit(node.getChildren().get(0), 0);
        Optional<String> right = visit(node.getChildren().get(1), 1);
        if (left.isPresent() && right.isPresent()) {
            node.removeJmmChild(0);
            node.removeJmmChild(1);
            JmmNode child;
            switch (node.get("op")) {
                case "+":
                    child = new JmmNodeImpl("Integer");
                    child.put("value",String.valueOf(Integer.parseInt(left.get()) + Integer.parseInt(right.get())));
                    node.getJmmParent().setChild(child,s);
                    return Optional.of(
                            String.valueOf(Integer.parseInt(left.get()) + Integer.parseInt(right.get())));
                case "-":
                    child = new JmmNodeImpl("Integer");
                    child.put("value",String.valueOf(Integer.parseInt(left.get()) - Integer.parseInt(right.get())));
                    node.getJmmParent().setChild(child,s);
                    return Optional.of(
                            String.valueOf(Integer.parseInt(left.get()) - Integer.parseInt(right.get())));
                case "*":
                    child = new JmmNodeImpl("Integer");
                    child.put("value",String.valueOf(Integer.parseInt(left.get()) * Integer.parseInt(right.get())));
                    node.getJmmParent().setChild(child,s);
                    return Optional.of(
                            String.valueOf(Integer.parseInt(left.get()) * Integer.parseInt(right.get())));
                case "/":
                    child = new JmmNodeImpl("Integer");
                    child.put("value",String.valueOf(Integer.parseInt(left.get()) / Integer.parseInt(right.get())));
                    node.getJmmParent().setChild(child,s);
                    return Optional.of(
                            String.valueOf(Integer.parseInt(left.get()) / Integer.parseInt(right.get())));
                case "<":
                    child = new JmmNodeImpl("Boolean");
                    child.put("value",String.valueOf(Integer.parseInt(left.get()) < Integer.parseInt(right.get())));
                    node.getJmmParent().setChild(child,s);
                    return Optional.of(
                            String.valueOf(Integer.parseInt(left.get()) < Integer.parseInt(right.get())));
                case ">":
                    child = new JmmNodeImpl("Boolean");
                    child.put("value",String.valueOf(Integer.parseInt(left.get()) > Integer.parseInt(right.get())));
                    node.getJmmParent().setChild(child,s);
                    return Optional.of(
                            String.valueOf(Integer.parseInt(left.get()) > Integer.parseInt(right.get())));
                case "==":
                    child = new JmmNodeImpl("Boolean");
                    child.put("value",String.valueOf(Integer.parseInt(left.get()) == Integer.parseInt(right.get())));
                    node.getJmmParent().setChild(child,s);
                    return Optional.of(left.get().equals(right.get()) ? "true" : "false");
                case "&&":
                    child = new JmmNodeImpl("Boolean");
                    child.put("value",String.valueOf(Boolean.parseBoolean(left.get()) && Boolean.parseBoolean(right.get())));
                    node.getJmmParent().setChild(child,s);
                    return Optional.of(
                            String.valueOf(Boolean.parseBoolean(left.get()) && Boolean.parseBoolean(right.get())));
                case "||":
                    child = new JmmNodeImpl("Boolean");
                    child.put("value",String.valueOf(Boolean.parseBoolean(left.get()) || Boolean.parseBoolean(right.get())));
                    node.getJmmParent().setChild(child,s);
                    return Optional.of(
                            String.valueOf(Boolean.parseBoolean(left.get()) || Boolean.parseBoolean(right.get())));
                default:
                    throw new RuntimeException("Unknown operator: " + node.get("op"));
            }
        }
        return Optional.empty();
    }
    private Optional<String> dealWithIdentifier(JmmNode node, Integer s) {
        String name = node.get("value");
        if(varValues.containsKey(name)){
            System.out.println("smvajfb");

            JmmNode child;
            if (isInteger(varValues.get(name)))
                child = new JmmNodeImpl("Integer");
            else
                child = new JmmNodeImpl("Boolean");
            child.put("value",varValues.get(name));
            node.getJmmParent().setChild(child,s);
            return Optional.of(varValues.get(name));
        }
        return Optional.empty();
    }

    private Optional<String> defaultVisit(JmmNode node, Integer s) {
        int numChildren = node.getNumChildren();
        for (int i = 0; i < node.getNumChildren(); i++) {
            visit(node.getChildren().get(i), i);
            if (node.getNumChildren() < numChildren) {
                i--;
                numChildren = node.getNumChildren();
            }
            if (node.getNumChildren() > numChildren) {
                i += node.getNumChildren() - numChildren;
                numChildren = node.getNumChildren();
            }
        }
        return Optional.empty();
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
