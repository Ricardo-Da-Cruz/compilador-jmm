package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;
import java.util.Map;

public class MethodSpecsGetter {
    private Method method;

    public String getMethodDefinition() {
        StringBuilder methodDefinition = new StringBuilder();

        if (method.isConstructMethod()) {
            method.setMethodName("<init>");
        }

        methodDefinition.append(getMethodHeader()).append("\n");

        StringBuilder instructions = new StringBuilder();

        this.method.buildVarTable();
        Translator instructionTranslator = new Translator();
        boolean hasReturn = false;

        for (Instruction instruction: method.getInstructions()) {
            if (!hasReturn && instruction.getInstType() == InstructionType.RETURN) {
                hasReturn = true;
            }

            for (Map.Entry<String, Instruction> entry: method.getLabels().entrySet()) {
                if (entry.getValue().equals(instruction)) {
                    instructions.append(entry.getKey()).append(":").append("\n");
                }
            }
            instructions.append(instructionTranslator.translateInstruction(instruction, method)).append("\n");
        }

        methodDefinition.append("\t.limit stack ").append(instructionTranslator.getMaxLoadCounter()).append("\n");
        methodDefinition.append("\t.limit locals ").append(this.getLocalsLimit()).append("\n");

        methodDefinition.append(instructions);

        if (!hasReturn) {
            methodDefinition.append("\t").append("return").append("\n");
        }

        methodDefinition.append(".end method\n");

        return methodDefinition.toString();
    }

    private String getMethodHeader() {
        StringBuilder methodHeader = new StringBuilder(".method ");

        if (method.getMethodAccessModifier().toString().equals("DEFAULT")) {
            methodHeader.append("public ");
        } else {
            methodHeader.append(method.getMethodAccessModifier().toString().toLowerCase()).append(" ");
        }

        if (method.isFinalMethod()) {
            methodHeader.append("final ");
        }

        if (method.isStaticMethod()) {
            methodHeader.append("static ");
        }

        methodHeader.append(method.getMethodName()).append(getMethodDescriptor());

        return methodHeader.toString();
    }

    public String getMethodDescriptor() {
        StringBuilder descriptor = new StringBuilder("(");

        for (Element parameter: method.getParams()) {
            descriptor.append(translateType(method.getOllirClass(), parameter.getType()));
        }

        descriptor.append(")").append(translateType(method.getOllirClass(), method.getReturnType()));

        return descriptor.toString();
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    private int getLocalsLimit() {

        if (this.method == null) {
            return 0;
        }

        return this.method.getVarTable().values().stream().mapToInt(Descriptor::getVirtualReg).max().orElse(0) + 1;
    }

    public static String translateType(ClassUnit oC, Type type) {
        ElementType elementType = type.getTypeOfElement();

        return switch (elementType) {
            case ARRAYREF -> "[" + translateType(((ArrayType) type).getArrayType());
            case OBJECTREF, CLASS -> {
                yield "L" + getFullClassName(oC, ((ClassType) type).getName()) + ";";
            }
            default -> translateType(elementType);
        };
    }

    private static String translateType(ElementType elementType) {
        return switch (elementType) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case STRING -> "Ljava/lang/String;";
            case THIS -> "this";
            case VOID -> "V";
            default -> "";
        };
    }

    public static String getFullClassName(ClassUnit oC, String className) {
        if (oC.isImportedClass(className)) {
            for (String fullImport : oC.getImports()) {
                int lastSeparatorIndex = className.lastIndexOf(".");

                if (lastSeparatorIndex < 0 && fullImport.equals(className)) {
                    return className;
                } else if (fullImport.substring(lastSeparatorIndex + 1).equals(className)) {
                    return fullImport;
                }
            }
        }

        return className;
    }

    public static String trim(String s) {
        if (s.charAt(0) != '"') {
            return s;
        }
        return s.length() == 1 ? s : s.substring(1, s.length() - 1);
    }
}
