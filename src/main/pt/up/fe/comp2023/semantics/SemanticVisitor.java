package pt.up.fe.comp2023.semantics;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.ClassTable;

import java.util.LinkedList;
import java.util.List;


/*
    * This class is responsible for the semantic analysis of the code.
    * s.a is the class name
    * s.b is the method name
    *
    * returns a response
    *   if there was an error the response will be false, always check if the response is false before using the type
    *   if there was no error the response will be true and the type will be a valid type or if unknown will be ANY
 */
public class SemanticVisitor extends AJmmVisitor<Pair<String,String>, Response> {

    private final ClassTable classTable;
    private final List<String> imports;
    private final List<Report> Errors = new LinkedList<>();
    private final Type anyType = new Type(Types.ANY.getName(), false);
    private final Type intType = new Type(Types.INT.getName(), false);
    private final Type boolType = new Type(Types.BOOLEAN.getName(), false);

    public SemanticVisitor(ClassTable classTable) {
        super();
        this.classTable = classTable;
        imports = classTable.getImports().stream().map(x -> x.split("\\.")[x.split("\\.").length-1]).toList();
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("ImportDeclaration", (node, s) -> new Response(true));
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("Method", this::dealWithMethod);
        addVisit("Statement", this::dealWithStatement);
            addVisit("Scope", this::dealWithScope);
            addVisit("IfElse", this::dealWithIfElse);
            addVisit("While", this::dealWithWhile);
            addVisit("Return", this::dealWithReturn);
            addVisit("ExpressionST", (node, s) -> visit(node.getJmmChild(0), s));
            addVisit("Assignment", this::dealWithAssignment);
            addVisit("ArrayAssignment", this::dealWithArrayAssignment);
            addVisit("DeclarationST", (node, s) -> visit(node.getJmmChild(0), s));
        addVisit("Expression", this::dealWithExpression);
            addVisit("BinaryOp", this::dealWithBinaryOp);
            addVisit("UnaryOp", this::dealWithUnaryOp);
            addVisit("NewArray", this::dealWithNewArray);
            addVisit("NewObject", (node, s) -> visit(node.getJmmChild(0), s));
            addVisit("FieldAccess", this::dealWithFieldAccess);
            addVisit("MethodCall", this::dealWithMethodCall);
            addVisit("ArrayAccess", this::dealWithArrayAccess);
            addVisit("ArrayLength", this::dealWithArrayLength);
                addVisit("Boolean", (node, s) -> new Response(boolType));
                addVisit("Integer", (node, s) -> new Response(intType));
                addVisit("Null", (node, s) -> new Response(anyType));
                addVisit("String", (node, s) -> new Response(new Type(Types.STRING.getName(), false)));
                addVisit("Char", (node, s) -> new Response(new Type(Types.CHAR.getName(), false)));
                addVisit("Identifier", this::dealWithIdentifier);
                addVisit("This", this::dealWithThis);
        addVisit("Declaration", this::dealWithDeclaration);
            addVisit("VariableDeclaration", (node, s) -> new Response(true));
        addVisit("Type", this::dealWithType);

        setDefaultVisit(this::defaultVisit);
    }

    /*
    * if the method where this is called is static then it will return an error
    * if the method where this is called is not static then it will return the type of the class
     */
    private Response dealWithThis(JmmNode node, Pair<String, String> s) {
        if (classTable.isStatic(s.b)){
            Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Cannot use this in a static context"));
            return new Response(false);
        }
        return new Response(new Type(s.a, false));
    }

    /*
    * if the identifier is an import then it will return the type of the import class
    * if the identifier is a variable then it will return the type of the variable
    * if the identifier is the class name then it will return the static type to show the methodCall method that it is calling a static method
    * if the Identifier is unknown and the class has a super class then it will return ANY
    * if the Identifier is unknown and the class has no super class then it will return an error
     */
    private Response dealWithIdentifier(JmmNode node, Pair<String, String> s) {
        var response = SearchVar(node, s, "value");
        if (response.getResult())
            return response;

        if (imports.contains(node.get("value")))
            return new Response(new Type(node.get("value"), false));

        if (classTable.getClassName().equals(node.get("value")))
            return new Response(new Type("static", false));

        if (classTable.hasSuper()){
            return new Response(anyType);
        }

        Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Variable " + node.get("value") + " not found"));
        return new Response(false);
    }

    /*
    * if the type is unknown then it will assume it is correct
    * if the type is static it will deal with it through the dealWithStaticCall method
    * if the type is the class name then it will deal with it through the dealWithNonStaticMethod method
    * if the type is an import then it will return ANY
    * else it will return an error
     */
    private Response dealWithMethodCall(JmmNode node, Pair<String, String> s) {
        var response = visit(node.getJmmChild(0), s);
        if (!response.getResult())
            return response;

        if (response.getType().getName().equals(Types.ANY.getName())) {
            return response;
        }

        if (response.getType().getName().equals("static")){
            return dealWithStaticCall(node, s);
        }

        if (response.getType().getName().equals(s.a)){
            return dealWithNonStaticMethod(node, s);
        }

        if (imports.contains(response.getType().getName())){
            return new Response(anyType);
        }

        Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Method " + node.get("value") + " not found"));
        return new Response(false);
    }

    /*
    * this method is called when the type of the method call is the class name
    * if the method is not in the class table then it will return an error if the class has no super class or ANY if it has a super class
    * if the method is static then it will return an error
    * the method will then check the argument types and return an error if they are not correct
     */
    private Response dealWithNonStaticMethod(JmmNode node, Pair<String, String> s) {
        if (classTable.getMethodType(node.get("value")) == null){
            return checkSuper(node);
        }else {
            if (classTable.isStatic(node.get("value"))){
                Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Cannot call static method " + node.get("value") + " from a non-static context"));
                return new Response(false);
            }

            var params = classTable.getParameters(node.get("value"));
            if (params.size() != node.getChildren().size() - 1) {
                Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Expected " + params.size() + " arguments but got " + (node.getChildren().size() - 1)));
                return new Response(false);
            }
            for (int i = 0; i < params.size(); i++) {
                var param = params.get(i);
                var arg = node.getJmmChild(i+1);
                var argResponse = visit(arg, s);
                if (!argResponse.getResult())
                    return argResponse;

                if (!argResponse.getType().equals(param.getType())) {
                    Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(arg.get("lineStart")), Integer.parseInt(arg.get("colStart")), "Expected " + param.getType() + " but got " + argResponse.getType().getName()));
                    return new Response(false);
                }
            }
            return new Response(classTable.getMethodType(node.get("value")));
        }
    }

    /*
     * this method is called when the type of the method call is the class name
     * if the method is not in the class table then it will return an error if the class has no super class or ANY if it has a super class
     * if the method is not static then it will return an error
     * the method will then check the argument types and return an error if they are not correct
     */
    private Response dealWithStaticCall(JmmNode node, Pair<String, String> s) {
        if (classTable.getMethodType(node.get("value")) == null){
            return checkSuper(node);
        }else {
            if (!classTable.isStatic(node.get("value"))){
                Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Cannot call non-static method " + node.get("value") + " from a static context"));
                return new Response(false);
            }

            var params = classTable.getParameters(node.get("value"));

            if (params.size() != node.getChildren().size() - 1) {
                Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Expected " + params.size() + " arguments but got " + (node.getChildren().size() - 1)));
                return new Response(false);
            }

            for (int i = 0; i < params.size(); i++) {
                var param = params.get(i);
                var arg = node.getJmmChild(i+1);
                var argResponse = visit(arg, s);
                if (!argResponse.getResult())
                    return argResponse;

                if (!argResponse.getType().equals(param.getType())){
                    Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Argument " + (i+1) + " of method " + node.get("value") + " is of type " + argResponse.getType().getName() + " but should be of type " + param.getName()));
                    return new Response(false);
                }
            }
            return new Response(classTable.getReturnType(node.get("value")));
        }
    }

    private Response dealWithFieldAccess(JmmNode node, Pair<String, String> s) {
        var response = visit(node.getJmmChild(0), s);
        if (!response.getResult())
            return response;

        if (response.getType().getName().equals(Types.ANY.getName())) {
            return response;
        }

        if (response.getType().isArray()){
            Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Cannot access field of an array"));
            return new Response(false);
        }

        if (response.getType().getName().equals(s.a) && classTable.isStatic(s.b)){
            Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Cannot access non-static field in a static context"));
            return new Response(false);
        }

        if (response.getType().getName().equals(s.a)){
            var type = classTable.getFieldType(node.get("value"));
            if (type == null){
                if (classTable.hasSuper()){
                    return new Response(anyType);
                }else{
                    Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Field " + node.get("value") + " not found"));
                    return new Response(false);
                }
            }
            return new Response(classTable.getFieldType(node.get("value")));
        }

        if (imports.contains(response.getType().getName())){
            return new Response(anyType);
        }

        Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Field " + node.get("value") + " not found"));
        return new Response(false);
    }

    private Response dealWithNewArray(JmmNode node, Pair<String, String> s) {
        Response type = visit(node.getChildren().get(0), s);
        Response size = visit(node.getChildren().get(1), s);
        if (!type.getResult() || !size.getResult())
            return new Response(false);
        if (type.getType().isArray()){
            Errors.add(new Report(ReportType.ERROR,Stage.SYNTATIC,Integer.parseInt(node.get("lineStart")),"Array initialization error"));
            return new Response(false);
        }
        if (
            !size.getType().equals(intType)
                &&
            !size.getType().equals(anyType)
        ) {
            Errors.add(new Report(ReportType.ERROR,Stage.SYNTATIC,Integer.parseInt(node.get("lineStart")),"Array size must be an integer"));
            return new Response(false);
        }
        return new Response(new Type(type.getType().getName(), true));
    }

    private Response dealWithArrayLength(JmmNode node, Pair<String, String> s) {
        Response Var = SearchVar(node, s,"value");
        if (!Var.getResult())
            return new Response(false);
        if (
            !Var.getType().isArray()
                &&
            !Var.getType().equals(anyType)
                &&
            !Var.getType().equals(new Type(Types.STRING.getName(), false))
        ) {
            Errors.add(new Report(ReportType.ERROR,Stage.SYNTATIC,Integer.parseInt(node.get("lineStart")),"Variable is not an array"));
            return new Response(false);
        }
        return new Response(intType);
    }

    private Response dealWithArrayAccess(JmmNode node, Pair<String, String> s) {
        Response Var = visit(node.getChildren().get(0), s);
        Response index = visit(node.getChildren().get(1), s);
        if (!checkType(node.getJmmChild(0),index,intType))
            return new Response(false);
        if (!Var.getResult())
            return new Response(false);
        if (!Var.getType().isArray()) {
            Errors.add(new Report(ReportType.ERROR,Stage.SYNTATIC,Integer.parseInt(node.get("lineStart")),"Variable is not an array"));
            return new Response(false);
        }
        return new Response(new Type(Var.getType().getName(), false));
    }

    private Response dealWithUnaryOp(JmmNode node, Pair<String, String> s) {
        Response v0 = visit(node.getChildren().get(0), s);
        if (node.get("op").equals("!")){
            if (!v0.getResult())
                return new Response(false);
            if (!v0.getType().equals(boolType)) {
                Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), "UnaryOp: " + node.get("op") + " can only be applied to boolean"));
                return new Response(false);
            }
            return new Response(boolType);
        }
        if (node.get("op").equals("-")){
            if (!v0.getResult())
                return new Response(false);
            if (!v0.getType().equals(intType)){
                Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), "UnaryOp: " + node.get("op") + " can only be applied to int"));
                return new Response(false);
            }
            return new Response(intType);
        }
        return new Response(false);
    }

    private Response dealWithBinaryOp(JmmNode node, Pair<String, String> s) {
        Response v0 = visit(node.getChildren().get(0), s);
        Response v1 = visit(node.getChildren().get(1), s);
        switch (node.get("op")) {
            case "+", "-", "*", "/" -> {
                if (!checkType(node.getChildren().get(0),v0, intType) || !checkType(node.getChildren().get(1),v1, intType))
                    return new Response(false);

                return new Response(intType);
            }
            case "&&", "||" -> {
                if (!checkType(node.getJmmChild(0),v0, boolType) || !checkType(node.getJmmChild(1),v1, boolType))
                    return new Response(false);
                return new Response(boolType);
            }
            case "==", "!=" -> {
                if (!v0.getResult() || !v1.getResult()){
                    return new Response(false);
                }
                return new Response(boolType);
            }
            case "<" -> {
                if (!checkType(node.getJmmChild(0),v0, intType) || !checkType(node.getJmmChild(1),v1, intType))
                    return new Response(false);
                return new Response(boolType);
            }
            default -> {
                Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), "BinaryOp: " + node.get("op") + " is not a valid operator"));
                return new Response(false);
            }
        }
    }

    private Response dealWithArrayAssignment(JmmNode node, Pair<String, String> s) {
        var acessIndex = visit(node.getJmmChild(0), s);
        var assignment = visit(node.getJmmChild(1), s);
        var variable = SearchVar(node,s,"var");
        if (!acessIndex.getResult() || !assignment.getResult() || !variable.getResult()) {
            return new Response(false);
        }
        if (
            !acessIndex.getType().equals(intType)
                &&
            !acessIndex.getType().equals(anyType)
        ) {
            Errors.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(node.get("lineStart")),Integer.parseInt(node.get("colStart")),String.format("Array index must be of type int, not %s",acessIndex.getType())));
            return new Response(false);
        }
        if (!variable.getType().isArray()) {
            Errors.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(node.get("lineStart")),Integer.parseInt(node.get("colStart")),String.format("Variable %s is not an array",node.get("var"))));
            return new Response(false);
        }
        if (
            isPrimitive(variable.getType())
                ||
            (variable.getType().getName().equals(s.a)
                &&
            !classTable.hasSuper())
        ) {
            if (
                !(new Type(variable.getType().getName(),false)).equals(assignment.getType())
                    &&
                !assignment.getType().equals(anyType)
            ) {
                Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), String.format("Variable %s is of type %s and cannot be assigned to a value of type %s", node.get("var"), variable.getType(), assignment.getType())));
                return new Response(false);
            }
        }
        if (
            variable.getType().getName().equals(s.a)
                &&
            classTable.hasSuper()
                &&
            !assignment.getType().getName().equals(s.a)
                &&
            !assignment.getType().equals(anyType)
                &&
            !classTable.getSuper().equals(assignment.getType().getName())
        ) {
            Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), String.format("Variable %s is of type %s and cannot be assigned to a value of type %s", node.get("var"), classTable.getLocalVariableType(s.b, node.get("var")), assignment.getType())));
            return new Response(false);
        }

        if (
                variable.getType().getName().equals(s.a)
                        &&
                        !classTable.hasSuper()
                        &&
                        !assignment.getType().getName().equals(s.a)
                        &&
                        !assignment.getType().equals(anyType)
        ) {
            Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), String.format("Variable %s is of type %s and cannot be assigned to a value of type %s", node.get("var"), classTable.getLocalVariableType(s.b, node.get("var")), assignment.getType())));
            return new Response(false);
        }


        if (assignment.getType().isArray()){
            Errors.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(node.get("lineStart")),Integer.parseInt(node.get("colStart")),String.format("Variable %s is of type %s and cannot be assigned to a value of type %s",node.get("var"),variable.getType(),assignment.getType())));
            return new Response(false);
        }
        return new Response(true);
    }

    private Response dealWithAssignment(JmmNode node, Pair<String, String> s) {
        var assignment = visit(node.getJmmChild(0), s);
        var variable = SearchVar(node,s,"var");
        if (!assignment.getResult() || !variable.getResult()) {
            return new Response(false);
        }
        if (!assignment.getType().equals(anyType)) {
            // if the variable is primitive and the assignment is not, the error
            if (
                isPrimitive(variable.getType())
                    &&
                !variable.getType().equals(assignment.getType())
            ) {
                Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), String.format("Variable %s is of type %s and cannot be assigned to a value of type %s", node.get("var"), classTable.getLocalVariableType(s.b, node.get("var")), assignment.getType())));
                return new Response(false);
            }
            // if the variable this class and is not extended by the assignment type, the error
            if (assignment.getType().getName().equals(s.a)) {
                if (
                    !variable.getType().equals(assignment.getType())
                        &&
                    !variable.getType().getName().equals(classTable.getSuper())
                ) {
                    Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), String.format("Variable %s is of type %s and cannot be assigned to a value of type %s", node.get("var"), classTable.getLocalVariableType(s.b, node.get("var")), assignment.getType())));
                    return new Response(false);
                }
            }
        }

        return new Response(true);
    }

    private Response dealWithReturn(JmmNode node, Pair<String, String> s) {
        var returnType = classTable.getReturnType(s.b);
        if (returnType.equals(new Type(Types.VOID.getName(), false))) {
            if (node.getChildren().size() == 0)
                return new Response(true);
            Errors.add(new Report(ReportType.ERROR,Stage.SYNTATIC,Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), String.format("Method %s in class %s should not return a value", s.b, s.a)));
            return new Response(false);
        }
        var expression = visit(node.getChildren().get(0), s);
        if (!expression.getResult()) {
            return expression;
        }
        if (returnType.equals(expression.getType()) || expression.getType().equals(anyType)) {
            return new Response(true);
        }
        Errors.add(new Report(ReportType.ERROR,Stage.SYNTATIC,Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), String.format("Method %s in class %s should return a %s instead of %s", s.b, s.a, classTable.getReturnType(s.b),expression.getType())));
        return new Response(false);
    }

    private Response dealWithWhile(JmmNode node, Pair<String, String> s) {
        var condition = visit(node.getJmmChild(0), s);
        var statements = visit(node.getJmmChild(1), s);
        if (!condition.getResult() || !statements.getResult()) {
            return new Response(false);
        }
        if (
            !condition.getType().equals(boolType)
                &&
            !condition.getType().equals(anyType)
        ) {
            Errors.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(node.get("lineStart")),Integer.parseInt(node.get("colStart")),"While condition must be a boolean expression"));
            return new Response(false);
        }
        return new Response(true);
    }

    private Response dealWithIfElse(JmmNode node, Pair<String, String> s) {
        var condition = visit(node.getJmmChild(0), s);
        var onTrue = visit(node.getJmmChild(1), s);

        if (node.getChildren().size() == 3){
            var onFalse = visit(node.getJmmChild(2), s);
            if (!onFalse.getResult())
                return new Response(false);
        }
        if (!condition.getResult() || !onTrue.getResult()) {
            return new Response(false);
        }
        if (
            !condition.getType().equals(boolType)
                &&
            !condition.getType().equals(anyType)
        ) {
            Errors.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(node.get("lineStart")),Integer.parseInt(node.get("colStart")),"If condition must be a boolean expression"));
            return new Response(false);
        }
        return new Response(true);
    }

    /*
     * visits the scope statements in the scope
     */
    private Response dealWithScope(JmmNode jmmNode, Pair<String, String> stringStringPair) {
        boolean result = true;
        for (JmmNode child : jmmNode.getChildren()) {
            result &= visit(child, stringStringPair).getResult();
        }
        return new Response(result);
    }

    /*
     * visits the class
     */
    private Response dealWithProgram(JmmNode node, Pair<String, String> s){
        boolean result = true;
        for (JmmNode child : node.getChildren()) {
            result &= visit(child, new Pair<>("", "")).getResult();
        }
        return new Response(result);
    }

    /*
     * visits all the class fields and methods
     */
    private Response dealWithClassDeclaration(JmmNode node, Pair<String, String> s){
        boolean result = true;
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Method")){
                result &= visit(child, new Pair<>(node.get("name"), "")).getResult();
            }
        }
        return new Response(result);
    }

    /*
    * returns the type of the node if it is a primitive type of imported class or the class itself
     */
    private Response dealWithType(JmmNode node, Pair<String, String> s) {

        if (node.getKind().equals("ArrayType")) {
            return new Response(new Type(visit(node.getJmmChild(0),s).getType().getName(), true));
        }
        if (node.getKind().equals("ClassType")) {
            if (imports.contains(node.get("value")) || node.get("value").equals(s.a)) {
                return new Response(new Type(node.get("value"), false));
            }else {
                Errors.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(node.get("lineStart")),Integer.parseInt(node.get("colStart")),String.format("Class %s not found", node.get("value"))));
                return new Response(false);
            }
        }
        return new Response(new Type(node.get("value"), false));
    }

    //calls all the statements in the method
    private Response dealWithMethod(JmmNode node, Pair<String, String> s){
        boolean result = true;
        for (JmmNode child : node.getChildren()) {
            result &= visit(child, new Pair<>(s.a, node.get("name"))).getResult();
        }
        return new Response(result);
    }

    //only called if there is an error
    private Response dealWithStatement(JmmNode node,Pair<String, String> s){
        Errors.add(new Report(ReportType.ERROR,Stage.SYNTATIC,Integer.parseInt(node.get("lineStart")),Integer.parseInt(node.get("colStart")),"Statement not supported"));
        return new Response(false);
    }

    /*
    *   Checks if the type of the variable is the same as the type of the expression
     */
    private Response dealWithDeclaration(JmmNode node, Pair<String, String> s) {
        if (!node.getKind().equals("Initialization")) {
            return new Response(true);
        }
        Response a = visit(node.getJmmChild(0), s);
        Response b = visit(node.getJmmChild(1), s);

        if (!checkType(node.getJmmChild(0),b,a.getType()))
            return new Response(false);

        return new Response(true);
    }

    //only called if there is an error
    private Response dealWithExpression(JmmNode node, Pair<String, String> s){
        Errors.add(new Report(ReportType.ERROR,Stage.SYNTATIC,Integer.parseInt(node.get("lineStart")),Integer.parseInt(node.get("colStart")),"Expression not supported"));
        return new Response(false);
    }

    //only called if there is an error
    private Response defaultVisit(JmmNode node, Pair<String, String> s){
        for (String a:node.getHierarchy())
            System.out.println("\t"+a);
        Errors.add(new Report(ReportType.ERROR, Stage.SYNTATIC, -1, String.format("Node %s not supported", node.getKind())));
        return new Response(false);
    }

    private Response SearchVar(JmmNode node, Pair<String, String> s,String name) {
        if (classTable.getLocalVariableType(s.b, node.get(name)) != null) {
            return new Response(classTable.getLocalVariableType(s.b, node.get(name)));
        }
        if (classTable.getParameterType(s.b, node.get(name)) != null) {
            return new Response(classTable.getParameterType(s.b, node.get(name)));
        }
        if (classTable.getFieldType(node.get(name)) != null) {
            if (classTable.isStatic(s.b)){
                Errors.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(node.get("lineStart")),Integer.parseInt(node.get("colStart")),String.format("Cannot access non-static variable %s from a static context",node.get(name))));
                return new Response(false);
            }
            return new Response(classTable.getFieldType(node.get(name)));
        }
        if (!imports.contains(node.get(name)) && !node.get(name).equals(s.a))
            Errors.add(new Report(ReportType.ERROR, Stage.SYNTATIC, Integer.parseInt(node.get("lineStart")), String.format("Variable %s not found", node.get(name))));

        return new Response(false);
    }

    private Response checkSuper(JmmNode node) {
        if (classTable.hasSuper()){
            return new Response(anyType);
        }else{
            Errors.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Method " + node.get("value") + " not found"));
            return new Response(false);
        }
    }

    private boolean checkType(JmmNode node,Response response, Type type){
        if (!response.getResult())
            return false;

        if (
            !response.getType().equals(type)
                &&
            !response.getType().equals(anyType)
        ){
            Errors.add(new Report(ReportType.ERROR,Stage.SEMANTIC,Integer.parseInt(node.get("lineStart")),Integer.parseInt(node.get("colStart")),String.format("Type %s is not compatible with %s",response.getType(),type)));
            return false;
        }

        return true;
    }

    private boolean isPrimitive(Type type){
        return  type.getName().equals(Types.INT.getName()) ||
                type.getName().equals(Types.BOOLEAN.getName()) ||
                type.getName().equals(Types.CHAR.getName()) ||
                type.getName().equals(Types.STRING.getName());
    }

    public List<Report> getErrors() {
        return Errors;
    }
}

