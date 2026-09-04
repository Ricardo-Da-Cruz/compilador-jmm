package pt.up.fe.comp2023.ollir;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.semantics.Types;

import java.util.List;
import java.util.Objects;

/*
    * This class is responsible for generating the ollir code for the expressions
    * in the method parameter pair<String,String> s,
    * s.a is the tabulation
    * s.b is the variable name
 */
public class OllirExpressionGenerator extends AJmmVisitor<Pair<String,Symbol>,Pair<StringBuilder, Symbol>> {

    private final SymbolTable symbolTable;
    private int tempVars = 0;
    private final String methodName;

    public OllirExpressionGenerator(SymbolTable symbolTable, String methodName) {
        this.symbolTable = symbolTable;
        this.methodName = methodName;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Expression", this::dealWithExpression);
            addVisit("BinaryOp", this::dealWithBinaryOp);
            addVisit("UnaryOp", this::dealWithUnaryOp);
            addVisit("NewArray", this::dealWithNewArray);
            //todo:implement newObject
            addVisit("NewObject", this::dealWithNewObject);
            addVisit("FieldAccess", this::dealWithFieldAccess);
            addVisit("MethodCall", this::dealWithMethodCall);
            addVisit("ArrayAccess", this::dealWithArrayAccess);
            addVisit("ArrayLength", this::dealWithArrayLength);

            addVisit("Parenthesis", (node, s) -> visit(node.getJmmChild(0), s));
            addVisit("Boolean", (node, s) -> new Pair<>(new StringBuilder(),new Symbol(new Type(Types.BOOLEAN.getName(),false),node.get("value"))));
            addVisit("Integer", this::dealwithInteger);
            addVisit("Null", (node, s) -> new Pair<>(new StringBuilder(),new Symbol(new Type(Types.NULL.getName(),false),node.get("value"))));
            addVisit("String", this::dealWithString);
            addVisit("Char", (node, s) -> new Pair<>(new StringBuilder(),new Symbol(new Type(Types.CHAR.getName(),false),node.get("value"))));
            addVisit("Identifier", this::dealwithIdentifier);
            addVisit("ClassType", (node, s) -> new Pair<>(new StringBuilder(),new Symbol(new Type(node.get("value"),false),node.get("value"))));
            //todo implement this
            addVisit("This", (node, s) -> new Pair<>(new StringBuilder(),new Symbol(new Type(symbolTable.getClassName(),false),"this")));
            addVisit("Type",this::dealWithType);

        setDefaultVisit(this::defaultVisit);
    }

    private Pair<StringBuilder, Symbol> dealwithInteger(JmmNode node, Pair<String, Symbol> s) {
        if (s.b.getName().isEmpty()) {
            return new Pair<>(new StringBuilder(),new Symbol(new Type(Types.INT.getName(),false),node.get("value")));
        }else{
            StringBuilder code = new StringBuilder(s.a).append(String.format("%s.i32 :=.i32 %s.i32;\n",s.b.getName(),node.get("value")));
            return new Pair<>(code,new Symbol(new Type(Types.INT.getName(),false),s.b.getName()));
        }
    }
    //todo: possible problem with tempVars
    private Pair<StringBuilder, Symbol> dealWithString(JmmNode node, Pair<String, Symbol> s) {
        StringBuilder code = new StringBuilder();

        String varName = s.b.getName().isEmpty()? "t" + tempVars : s.b.getName();
        if (s.b.getName().isEmpty()) tempVars++;
        code.append(s.a)
            .append(
                String.format(
                    "%s%s :=%s ldc(%s)%s;\n",
                    varName,
                    getOllirType(new Type(Types.STRING.getName(),false)),
                    getOllirType(new Type(Types.STRING.getName(),false)),
                    node.get("value"),
                    getOllirType(new Type(Types.STRING.getName(),false))
                )
            );

        return new Pair<>(code,new Symbol(new Type(Types.STRING.getName(),false), varName));
    }

    private Pair<StringBuilder, Symbol> dealWithType(JmmNode jmmNode, Pair<String, Symbol> s) {
        if (jmmNode.getKind().equals("ArrayType")){
            return new Pair<>(new StringBuilder(),new Symbol(new Type(jmmNode.getJmmChild(0).get("value"),true),jmmNode.get("value")));
        }

        return new Pair<>(new StringBuilder(),new Symbol(new Type(jmmNode.get("value"),false),jmmNode.get("value")));
    }

    private Pair<StringBuilder, Symbol> dealWithNewObject(JmmNode node, Pair<String, Symbol> s) {
        StringBuilder code = new StringBuilder();
        String varName = s.b.getName().isEmpty()? "t" + tempVars : s.b.getName();
        if (s.b.getName().isEmpty())
            tempVars++;

        int myTempVar = tempVars;
        Type type = visit(node.getJmmChild(0),null).b.getType();

        code.append(s.a)
            .append(
                    String.format(
                            "%s%s :=%s new (%s)%s;\n",
                            varName,
                            getOllirType(type),
                            getOllirType(type),
                            type.getName(),
                            getOllirType(type))
            )
            .append(s.a)
            .append(
                    String.format(
                            "invokespecial(%s%s,\"<init>\").V;\n"
                            ,varName
                            ,getOllirType(type)
                    )
            );

        tempVars = myTempVar;

        return new Pair<>(code,new Symbol(s.b.getType(), varName));
    }

    private Pair<StringBuilder, Symbol> defaultVisit(JmmNode jmmNode, Pair<String, Symbol> s) {
        System.out.println("defaultVisit: " + jmmNode.getKind());
        System.out.println(jmmNode.getAttributes());
        throw new RuntimeException("defaultVisit: " + jmmNode.getKind());
    }

    private Pair<StringBuilder,Symbol> dealwithIdentifier(JmmNode node, Pair<String,Symbol> s) {
        StringBuilder code = new StringBuilder();
        Symbol symbol = generateVarSymbol(node.get("value"));
        if (symbol == null) {
            Symbol symbol1 = new Symbol(new Type(node.get("value"),false ),node.get("value"));
            return new Pair<>(code,symbol1);
        }
        return new Pair<>(code,symbol);
    }

    /*
        * This method is responsible for generating the ollir code for the array creation
        * returns a variable with the value of the array at the index of type of the array
     */
    private Pair<StringBuilder,Symbol> dealWithNewArray(JmmNode node, Pair<String,Symbol> s) {
        StringBuilder code = new StringBuilder();
        String varName = s.b.getName().isEmpty()? "t" + tempVars : s.b.getName();
        if (s.b.getName().isEmpty())
            tempVars++;
        int myTempVar = tempVars;

        Pair<StringBuilder,Symbol> visit = visit(
                node.getJmmChild(0),
                new Pair<>(s.a,new Symbol(new Type(s.b.getName(),false),""))
            );

        code.append(visit.a)
            .append(s.a)
            .append(
                String.format(
                    "%s%s :=%s new (array,%s.i32)%s;\n",
                    varName,
                    getOllirType(s.b.getType()),
                    getOllirType(s.b.getType()),
                    visit.b.getName(),
                    getOllirType(s.b.getType())
                )
            );

        tempVars = myTempVar;

        return new Pair<>(code,new Symbol(s.b.getType(), varName));
    }

    /*
        * This method is responsible for generating the ollir code for the array access
        * returns a variable with the value of the array at the index of type of the array
     */
    private Pair<StringBuilder,Symbol> dealWithArrayAccess(JmmNode node, Pair<String,Symbol> s) {
        StringBuilder code = new StringBuilder();
        String varName = s.b.getName().isEmpty()? "t" + tempVars : s.b.getName();
        if (s.b.getName().isEmpty())
            tempVars++;
        int myTempVar = tempVars;
        Pair<StringBuilder,Symbol> index =
                visit(
                        node.getJmmChild(1),
                        new Pair<>(s.a,new Symbol(new Type(Types.INT.getName(),false),"t" + tempVars))
                );
        Pair<StringBuilder,Symbol> accessed =
            visit(
                node.getJmmChild(0),
                new Pair<>(s.a,new Symbol(new Type(s.b.getName(),true),"")));

        code.append(accessed.a)
            .append(index.a)
            .append(s.a);


        code.append(
                String.format(
                        "%s%s :=%s %s[%s.i32]%s;\n",
                        varName,
                        getOllirType(accessed.b.getType()),
                        getOllirType(accessed.b.getType()),
                        accessed.b.getName(),
                        index.b.getName(),
                        getOllirType(accessed.b.getType())
                )
        );

        tempVars = myTempVar;
        return new Pair<>(code,new Symbol(accessed.b.getType(), varName));
    }

    /*
     * This method is responsible for generating the ollir code for the array length
     * returns a variable with the length of the array of type int
     */

    private Pair<StringBuilder,Symbol> dealWithArrayLength(JmmNode node, Pair<String,Symbol> s) {
        StringBuilder code = new StringBuilder();

        String varName = s.b.getName().isEmpty()? "t" + tempVars : s.b.getName();
        if (s.b.getName().isEmpty())
            tempVars++;
        int myTempVar = tempVars;

        Symbol array = visit(node.getJmmChild(0),null).b;



        code.append(s.a)
            .append(
                String.format(
                    "%s.i32 :=.i32 arraylength(%s%s).i32;\n",
                    varName,
                    array.getName(),
                    getOllirType(array.getType())
                )
            );

        tempVars = myTempVar;
        return new Pair<>(code,new Symbol(new Type(Types.INT.getName(), false), varName));
    }

    private Pair<StringBuilder,Symbol> dealWithFieldAccess(JmmNode node, Pair<String,Symbol> s) {
        StringBuilder code = new StringBuilder();

        String varName = s.b.getName().isEmpty()? "t" + tempVars : s.b.getName();
        if (s.b.getName().isEmpty())
            tempVars++;
        int myTempVar = tempVars;

        Pair<StringBuilder,Symbol> accessed = visit(
            node.getJmmChild(0),
            new Pair<>(
                "",
                new Symbol(new Type(varName,false),"")
            )
        );




        code.append(accessed.a)
            .append(s.a)
            .append(
                String.format(
                    "%s%s :=%s getfield(%s,%s%s)%s;\n",
                    varName,
                    getOllirType(s.b.getType()),
                    getOllirType(s.b.getType()),
                    accessed.b.getName(),
                        node.get("value"),
                        getOllirType(accessed.b.getType()),
                    getOllirType(s.b.getType())
                )
            );

        tempVars = myTempVar;
        return new Pair<>(code,new Symbol(s.b.getType(), varName));
    }

    private Pair<StringBuilder,Symbol> dealWithMethodCall(JmmNode node, Pair<String,Symbol> s) {
        StringBuilder code = new StringBuilder();
        String varName = s.b.getName().isEmpty()? "t" + tempVars : s.b.getName();
        if (s.b.getName().isEmpty())
            tempVars++;
        int myTempVar = tempVars;

        Pair<StringBuilder,Symbol> accessed = visit(
                    node.getJmmChild(0),
                    new Pair<>(s.a,new Symbol(new Type(s.b.getName(),true),""))
            );

        StringBuilder args = new StringBuilder();
        var method = symbolTable.getReturnType(node.get("value"));

        for (int i = 1; i < node.getChildren().size(); i++) {
            Pair<StringBuilder,Symbol> arg;
            if (method != null && accessed.b.getName().equals(symbolTable.getClassName())){
                 arg = visit(node.getJmmChild(i), new Pair<>(s.a,symbolTable.getParameters(node.get("value")).get(i-1)));
            }else {
                arg = visit(node.getJmmChild(i), new Pair<>(s.a, new Symbol(new Type("", false), "")));
            }
            code.append(arg.a);

            args.append(arg.b.getName())
                    .append(getOllirType(arg.b.getType()))
                    .append(",");
        }

        Type returnType;

        if (accessed.b.getType().equals(new Type(symbolTable.getClassName(),false)))
            returnType = symbolTable.getReturnType(node.get("value"));
        else
            returnType = s.b.getType();

        boolean isStatic = Objects.equals(accessed.b.getName(), accessed.b.getType().getName());

        code.append(accessed.a)
            .append(s.a);

        String invokename;

        if (isStatic) {
            invokename = accessed.b.getName();
        } else {
            if (accessed.b.getName().equals("this"))
                invokename = accessed.b.getName();
            else
                invokename = accessed.b.getName() + getOllirType(accessed.b.getType());
        }

        if (!s.b.getName().equals(Types.VOID.getName()))
            code.append(String.format("%s%s :=%s ", varName, getOllirType(returnType), getOllirType(returnType)));
        code.append(
            String.format(
                "%s(%s, \"%s\"%s)%s;\n",
                isStatic ? "invokestatic" : "invokevirtual",
                invokename,
                node.get("value"),
                args.length() > 0 ? ", "+args.substring(0, args.length() - 1) : "",
                getOllirType(returnType)
            )
        );

        tempVars = myTempVar;
        return new Pair<>(code,new Symbol(s.b.getType(), varName));
    }

    private Pair<StringBuilder,Symbol> dealWithUnaryOp(JmmNode node, Pair<String,Symbol> s) {
        StringBuilder code = new StringBuilder();

        Type type = new Type((node.get("op").equals("!")? Types.INT.getName() : Types.BOOLEAN.getName()), false);
        int myTempVar;
        String varName = s.b.getName().isEmpty()? "t" + tempVars : s.b.getName();
        if (s.b.getName().isEmpty()){
            tempVars++;
            myTempVar = tempVars + 1;
        }else {
            myTempVar = tempVars;
        }

        Pair<StringBuilder,Symbol> accessed = visit(node.getJmmChild(0), new Pair<>(s.a,new Symbol(type,"")));

        code.append(accessed.a)
            .append(s.a)
            .append(
            String.format(
                "%s%s :=%s %s%s %s%s;\n",
                varName,
                getOllirType(type),
                getOllirType(type),
                node.get("op"),
                getOllirType(type),
                accessed.b.getName(),
                getOllirType(type)
            )
        );

        tempVars = myTempVar;
        return new Pair<>(code,new Symbol(s.b.getType(), varName));
    }

    private Pair<StringBuilder,Symbol> dealWithBinaryOp(JmmNode node, Pair<String,Symbol> s) {
        StringBuilder code = new StringBuilder();

        Operators op = null;
        for (Operators i: Operators.values()){
            if (i.getLabel().equals(node.get("op")))
                op = i;
        }

        assert op != null;
        Type type = switch (op) {
            case ADD, SUB, MUL, DIV, LT -> new Type(Types.INT.getName(), false);
            case EQ, NEQ -> new Type("", false);
            case AND, OR -> new Type(Types.BOOLEAN.getName(), false);
        };
        Type returnType = switch (op){
            case EQ,NEQ,AND,OR,LT -> new Type(Types.BOOLEAN.getName(), false);
            case ADD,SUB,MUL,DIV -> new Type(Types.INT.getName(), false);
        };


        String varName = s.b.getName().isEmpty()? "t" + tempVars : s.b.getName();
        if (s.b.getName().isEmpty())
            tempVars++;
        int myTempVar = tempVars;


        Pair<StringBuilder,Symbol> left = visit(node.getJmmChild(0), new Pair<>(s.a, new Symbol(type,"")));
        Pair<StringBuilder,Symbol> right = visit(node.getJmmChild(1), new Pair<>(s.a, new Symbol(type,"")));

        code.append(left.a)
            .append(right.a)
            .append(s.a)
            .append(
                String.format(
                    "%s%s :=%s %s%s %s%s %s%s;\n",
                    varName,
                    getOllirType(returnType),
                    getOllirType(returnType),
                    left.b.getName(),
                    getOllirType(left.b.getType()),
                    node.get("op"),
                    getOllirType(left.b.getType()),
                    right.b.getName(),
                    getOllirType(left.b.getType())
                )
            );

        tempVars = myTempVar;
        return new Pair<>(code,new Symbol(returnType , varName));
    }

    private Pair<StringBuilder,Symbol> dealWithExpression(JmmNode node, Pair<String,Symbol> s) {
        System.out.println(node.getAttributes());
        System.out.println(node.getKind());
        throw new RuntimeException("Expression not supported");
    }


    private Symbol findSymbol(String name) {
        Symbol symbol = findVar(symbolTable.getLocalVariables(methodName),name);
        if (symbol != null)
            return symbol;
        symbol = findVar(symbolTable.getFields(),name);
        if (symbol != null)
            return symbol;
        symbol = findVar(symbolTable.getParameters(methodName),name);
        return symbol;
    }

    private Symbol findVar(List<Symbol> vars, String name){
        for (Symbol var : vars) {
            if (var.getName().equals(name))
                return var;
        }
        return null;
    }

    private String getOllirType(Type type) {
        Types types = null;
        for (Types mytype : Types.values()) {
            if (mytype.getName().equalsIgnoreCase(type.getName())) {
                types = mytype;
                break;
            }
        }


        if (types == null){
            return (type.isArray() ? OllirTypes.ARRAY.getLabel() : "") + "." + type.getName();
        }
        return (type.isArray() ? OllirTypes.ARRAY.getLabel() : "") + switch (types){
            case INT -> OllirTypes.INT.getLabel();
            case BOOLEAN -> OllirTypes.BOOLEAN.getLabel();
            case STRING -> OllirTypes.STRING.getLabel();
            case VOID -> OllirTypes.VOID.getLabel();
            default -> type.getName();
        };
    }

    private Symbol generateVarSymbol(String name) {
        var localVar = symbolTable.getLocalVariables(methodName).stream().filter(i -> i.getName().equals(name)).findFirst();
        if (localVar.isPresent())
            return localVar.get();

        List<Symbol> list = symbolTable.getParameters(methodName);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(name))
                return new Symbol(list.get(i).getType(), "$" + i + "." + name);
        }

        var field = symbolTable.getFields().stream().filter(i -> i.getName().equals(name)).findFirst();
        return field.orElse(null);

    }

}
