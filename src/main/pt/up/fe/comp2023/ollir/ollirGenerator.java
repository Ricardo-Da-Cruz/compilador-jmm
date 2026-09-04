package pt.up.fe.comp2023.ollir;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.semantics.Types;

import java.util.List;


/*
    * This class is responsible for generating the ollir code for the expressions
    * in the method parameter pair<String,String> s, the first string is the "\t"
    * and the second is the method name
    * returns a StringBuilder with the ollir code
 */
 class OllirGenerator extends AJmmVisitor<Pair<String,String>,StringBuilder>{

    private final SymbolTable symbolTable;



    OllirGenerator(SymbolTable symbolTable) {
        super();

        this.symbolTable = symbolTable;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("ImportDeclaration", (node, s) -> new StringBuilder());
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("Method", this::dealWithMethod);
        addVisit("Statement", this::dealWithStatement);
        addVisit("Scope", this::dealWithScope);
        addVisit("IfElse", this::dealWithIfElse);
        addVisit("While", this::dealWithWhile);
        addVisit("Return", this::dealWithReturn);

        addVisit("ExpressionST", this::dealWithExpressionST);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
        addVisit("DeclarationST", (node, s) -> visit(node.getJmmChild(0), s));

        addVisit("VariableDeclaration", (node, s) -> new StringBuilder());
        addVisit("Initialization", this::dealWithInitialization);
        addVisit("Type", (node, s) -> new StringBuilder());

        setDefaultVisit(this::defaultVisit);
    }

    private StringBuilder dealWithInitialization(JmmNode node, Pair<String, String> arg) {
        StringBuilder code = new StringBuilder();

        Symbol var = generateVarSymbol(node.get("name"), arg.b);
        assert var != null;

        OllirExpressionGenerator ollirExpressionGenerator = new OllirExpressionGenerator(symbolTable, arg.b);
        var value = ollirExpressionGenerator.visit(node.getJmmChild(1), new Pair<>(arg.a,var));

        code.append(value.a);

        if (!var.getName().equals(value.b.getName()))
            code.append(arg.a)
                    .append(
                            String.format(
                                    "%s%s :=%s %s%s;\n",
                                    var.getName(),
                                    getOllirType(var.getType()),
                                    getOllirType(var.getType()),
                                    value.b.getName(),
                                    getOllirType(value.b.getType())
                            )
                    );

        return code;
    }


    private StringBuilder dealWithExpressionST(JmmNode node, Pair<String, String> s) {
        OllirExpressionGenerator expressionGenerator = new OllirExpressionGenerator(symbolTable, s.b);
        var expression = expressionGenerator.visit(node.getJmmChild(0), new Pair<>(s.a, new Symbol(new Type("void", false),"void")));
        return new StringBuilder(expression.a);
    }

    //this is only called if there is an error
    private StringBuilder defaultVisit(JmmNode node, Pair<String, String> s) {
        System.out.println(node.getAttributes());
        System.out.println("Default visit: " + node.getKind());
        throw new RuntimeException("Default visit: " + node.getKind());
    }

    private StringBuilder dealWithArrayAssignment(JmmNode node, Pair<String, String> s) {
        OllirExpressionGenerator expressionGenerator = new OllirExpressionGenerator(symbolTable,s.b);

        Symbol symbol = generateVarSymbol(node.get("var"),s.b);
        assert symbol != null;

        StringBuilder code = new StringBuilder();
        Pair<StringBuilder, Symbol> index = expressionGenerator.visit(node.getJmmChild(0), new Pair<>(s.a, new Symbol(new Type("int", false),"t1")));
        Pair<StringBuilder, Symbol> value = expressionGenerator.visit(node.getJmmChild(1), new Pair<>(s.a, new Symbol(new Type(symbol.getType().getName(),false),"")));

        code.append(index.a)
            .append(value.a)
            .append(s.a)
            .append(
                    String.format(
                            "%s[%s.i32]%s :=%s %s%s;\n",
                            symbol.getName(),
                            index.b.getName(),
                            getOllirType(new Type(symbol.getType().getName(),false)),
                            getOllirType(new Type(symbol.getType().getName(),false)),
                            value.b.getName(),
                            getOllirType(new Type(symbol.getType().getName(),false))
                    )
            );

        return code;
    }

     private StringBuilder dealWithProgram(JmmNode node, Pair<String,String> arg) {
        StringBuilder code = new StringBuilder();

        //add imports
         for (String a:symbolTable.getImports()){
             code.append(String.format("import %s;\n",a));
         }

        for (JmmNode child : node.getChildren()) {
            code.append(visit(child, arg));
        }
        return code;
    }

    private StringBuilder dealWithClassDeclaration(JmmNode node, Pair<String,String> arg) {
        StringBuilder code = new StringBuilder();


        if (node.hasAttribute("superClass")){
            code.append(String.format("%s extends %s {\n",node.get("name"),node.get("superClass")));
        }else {
            code.append(String.format("%s {\n", node.get("name")));
        }

        // Add fields
        for (Symbol symbol : symbolTable.getFields()) {
            code.append("\t.field ")
                .append(AccessModifiers.PRIVATE.getLabel())
                .append(" ")
                .append(symbol.getName())
                .append(getOllirType(symbol.getType()))
                .append(";\n");
        }

        code.append(arg.a).append("\t")
                .append(String.format(".construct %s().V {\n" +
                " \t\tinvokespecial(this, \"<init>\").V;\n" +
                "\t}\n",symbolTable.getClassName()));

        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Method"))
                code.append(visit(child, new Pair<>("\t","")));
        }

        return code.append("}\n");
    }

    private StringBuilder dealWithMethod(JmmNode node, Pair<String,String> arg) {
        StringBuilder code = new StringBuilder(arg.a);
        if (node.get("name").equals(symbolTable.getClassName()))
            code.append(arg.a).append(".constructor");
        else
            code.append(".method");

        switch (AccessModifiers.valueOf(node.get("visibility").toUpperCase())) {
            case PUBLIC -> code.append(" ").append(AccessModifiers.PUBLIC.getLabel()).append(" ");
            case PRIVATE -> code.append(" ").append(AccessModifiers.PRIVATE.getLabel()).append(" ");
            case PROTECTED -> code.append(" ").append(AccessModifiers.PROTECTED.getLabel()).append(" ");
            default -> code.append(AccessModifiers.DEFAULT.getLabel());
        }

        if (node.hasAttribute("istatic")){
            code.append(NonAcessModifiers.STATIC.getLabel()).append(" ");
        }

        code.append(node.get("name"))
            .append("(");

        if (node.get("name").equals(symbolTable.getClassName()))
            code.append("invokespecial(this, \"<init>\").V;");
        for(Symbol symbol : symbolTable.getParameters(node.get("name"))) {
            code.append(symbol.getName()).append(getOllirType(symbol.getType())).append(", ");
        }
        if (symbolTable.getParameters(node.get("name")).size() > 0){
            code.deleteCharAt(code.length() - 1);
            code.deleteCharAt(code.length() - 1);
        }

        code.append(")")
            .append(getOllirType(symbolTable.getReturnType(node.get("name"))))
            .append(" {\n");

        for(JmmNode child : node.getChildren()) {
            if (!child.getKind().equals("Declaration"))
                code.append(visit(child, new Pair<>(arg.a + "\t",node.get("name"))));
        }

        if (code.toString().endsWith("End:\n"))
            code.delete(code.length() - 5, code.length());

        if (code.toString().endsWith("endif:\n"))
            code.delete(code.length() - 7, code.length());


        return code.append(arg.a).append("}\n");
    }

    private StringBuilder dealWithStatement(JmmNode node, Pair<String,String> arg) {
        throw new UnsupportedOperationException("statement not supported");
    }

    private StringBuilder dealWithScope(JmmNode node, Pair<String,String> arg) {
        StringBuilder code = new StringBuilder();
        for (JmmNode child : node.getChildren()) {
            code.append(visit(child, arg));
        }
        return code;
    }

    private StringBuilder dealWithIfElse(JmmNode node, Pair<String,String> arg) {
        OllirExpressionGenerator expressionGenerator = new OllirExpressionGenerator(symbolTable,arg.b);

        var condition = expressionGenerator.visit(node.getJmmChild(0), new Pair<>(arg.a, new Symbol(new Type("boolean", false), "")));

        StringBuilder code = new StringBuilder(condition.a);

        code.append(arg.a)
            .append(String.format("if (%s%s) goto Body;\n",condition.b.getName(),getOllirType(condition.b.getType())))
            .append(arg.a)
            .append(node.getChildren().size() == 3? "goto else;\n" : "goto endif;\n")
            .append(arg.a).append("Body:\n").append(visit(node.getJmmChild(1), new Pair<>(arg.a + "\t", arg.b)));

        if (node.getChildren().size() == 3) {
            code.append(arg.a)
                .append("else:\n")
                .append(visit(node.getJmmChild(2), new Pair<>(arg.a + "\t", arg.b)));
        }

        return code.append(arg.a).append("endif:\n");
    }

    private StringBuilder dealWithReturn(JmmNode node, Pair<String,String> arg) {
        StringBuilder code = new StringBuilder();
        if (node.getChildren().size() > 0) {
            OllirExpressionGenerator ollirExpressionGenerator = new OllirExpressionGenerator(symbolTable, arg.b);
            var expression = ollirExpressionGenerator.visit(node.getJmmChild(0), new Pair<>(arg.a, new Symbol(symbolTable.getReturnType(arg.b),"")));
            code.append(expression.a)
                .append(arg.a)
                .append(
                    String.format(
                        "ret%s %s%s;\n",
                        getOllirType(expression.b.getType()),
                        expression.b.getName(),
                        getOllirType(expression.b.getType())
                    )
                );
        }else {
            code.append(arg.a)
                .append("return;\n");
        }
        return code;
    }

    private StringBuilder dealWithWhile(JmmNode node, Pair<String,String> arg) {
        OllirExpressionGenerator expressionGenerator = new OllirExpressionGenerator(symbolTable,arg.b);

        var condition = expressionGenerator
                .visit(node.getJmmChild(0), new Pair<>(arg.a + "\t", new Symbol(new Type("boolean", false), "")));

        return new StringBuilder(arg.a)
            .append("Loop:\n")
            .append(condition.a)
            .append(arg.a)
            .append(String.format("\tif (%s.bool ==.bool false.bool) goto End;\n",condition.b.getName()))
            .append(arg.a)
            .append("goto End;\n")
            .append(visit(node.getJmmChild(1), new Pair<>(arg.a + "\t", arg.b)))
            .append(arg.a)
            .append("goto Loop;\n")
            .append(arg.a)
            .append("End:\n");
    }

    private StringBuilder dealWithAssignment(JmmNode node, Pair<String,String> arg) {
        StringBuilder code = new StringBuilder();

        Symbol var = generateVarSymbol(node.get("var"), arg.b);
        assert var != null;


        OllirExpressionGenerator ollirExpressionGenerator = new OllirExpressionGenerator(symbolTable, arg.b);
        var value = ollirExpressionGenerator.visit(node.getJmmChild(0), new Pair<>(arg.a,var));

        code.append(value.a);

        if (!var.getName().equals(value.b.getName()))
            code.append(arg.a)
                .append(
                        String.format(
                                "%s%s :=%s %s%s;\n",
                                var.getName(),
                                getOllirType(var.getType()),
                                getOllirType(var.getType()),
                                value.b.getName(),
                                getOllirType(value.b.getType())
                        )
                );

        return code;
    }


    private String getOllirType(Type type) {
        Types types = null;
        for (Types mytype : Types.values()) {
            if (mytype.getName().equalsIgnoreCase(type.getName())) {
                types = mytype;
                break;
            }
        }

        if (types == null)
            return "." + type.getName();

        return (type.isArray() ? OllirTypes.ARRAY.getLabel() : "") + switch (types){
            case INT -> OllirTypes.INT.getLabel();
            case BOOLEAN -> OllirTypes.BOOLEAN.getLabel();
            case STRING -> OllirTypes.STRING.getLabel();
            case VOID -> OllirTypes.VOID.getLabel();
            default -> type.getName();
        };
    }

    private Symbol generateVarSymbol(String name, String methodName) {
        var localVar = symbolTable.getLocalVariables(methodName).stream().filter(i -> i.getName().equals(name)).findFirst();
        if (localVar.isPresent())
            return localVar.get();

        List<Symbol> list = symbolTable.getParameters(methodName);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(name))
                return new Symbol(list.get(i).getType(), "$" + i + "." + name);
        }

        return symbolTable.getFields().stream().filter(i -> i.getName().equals(name)).findFirst().orElse(null);

    }


}

