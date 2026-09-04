package pt.up.fe.comp2023.ollir;

import org.specs.comp.ollir.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OllirOptimizer {

        private ClassUnit classUnit;

        public OllirOptimizer(ClassUnit classUnit) {
            this.classUnit = classUnit;
        }


        public ClassUnit optimize(){
            ClassUnit optimizedClass = new ClassUnit();
            optimizedClass.setClassAccessModifier(classUnit.getClassAccessModifier());
            optimizedClass.setSuperClass(classUnit.getSuperClass());
            optimizedClass.setClassName(classUnit.getClassName());

            if (optimizedClass.isStaticClass())optimizedClass.setStaticClass();
            if (optimizedClass.isFinalClass())optimizedClass.setFinalClass();

            classUnit.getFields().forEach(optimizedClass::addField);
            classUnit.getMethods().forEach(method -> optimizedClass.addMethod(optimizeMethod(method)));

            return optimizedClass;
        }

        private Method optimizeMethod(Method method) {
            Method optimizedMethod = new Method(method.getOllirClass());
            optimizedMethod.setReturnType(method.getReturnType());
            optimizedMethod.setMethodName(method.getMethodName());
            method.getParams().forEach(optimizedMethod::addParam);
            optimizedMethod.setMethodAccessModifier(method.getMethodAccessModifier());

            if (method.isStaticMethod())
                optimizedMethod.setStaticMethod();
            if (method.isFinalMethod())
                optimizedMethod.setFinalMethod();
            if (method.isConstructMethod())
                optimizedMethod.setConstructMethod();

            optimizeIntr(optimizedMethod, method.getInstructions());


            return optimizedMethod;

        }

    private List<Instruction> optimizeIntr(Method method, List<Instruction> instructions) {
        Map<String,String> varTable = new HashMap<>();
        for (Instruction intr : instructions) {
            intr.show();

            switch (intr.getInstType()){
                case ASSIGN -> {
                    if (!DealWithAssign((AssignInstruction) intr,varTable)){
                        method.addInstr(intr);
                    }
                }
                default -> method.addInstr(intr);
            }
        }

        return instructions;

    }

    private boolean DealWithAssign(AssignInstruction intr,Map<String,String> varTable) {
        if (intr.getRhs().getInstType().equals(InstructionType.BINARYOPER)) {
            return dealWithBinaryOp(((Operand) intr.getDest()).getName(),((BinaryOpInstruction) intr.getRhs()), varTable);
        }
        if (intr.getRhs().getInstType().equals(InstructionType.UNARYOPER)) {
            return dealWithUnaryOp(((Operand) intr.getDest()).getName(),((UnaryOpInstruction) intr.getRhs()), varTable);
        }
        if (intr.getRhs().getInstType().equals(InstructionType.NOPER)) {
            return dealWithNoper(((Operand) intr.getDest()).getName(),((SingleOpInstruction) intr.getRhs()), varTable);
        }
        System.out.println("Invalid Instruction");
        intr.show();
        throw new RuntimeException("Invalid Instruction");
    }

    private boolean dealWithNoper(String destVar, SingleOpInstruction value, Map<String, String> varTable) {
        if (value.getSingleOperand().isLiteral()){
            varTable.put(destVar,((LiteralElement) value.getSingleOperand()).getLiteral());
            return true;
        }
        if (varTable.containsKey(((Operand) value.getSingleOperand()).getName())){
            varTable.put(destVar,varTable.get(((Operand) value.getSingleOperand()).getName()));
            return true;
        }
        return false;
    }

    private static boolean dealWithUnaryOp(String destVar,UnaryOpInstruction unaryOp, Map<String, String> varTable) {
        String value = "";
        if (!unaryOp.getOperand().isLiteral()) {
            value = ((LiteralElement) unaryOp.getOperand()).getLiteral();
        } else {
            Operand op = ((Operand) unaryOp.getOperand());
            if (varTable.containsKey(op.getName())) {
                value = varTable.get(op.getName());
            }
        }
        if (!value.isEmpty()) {
            varTable.put(destVar, "" +
                    switch (unaryOp.getOperation().getOpType()) {
                        case NOT -> !Boolean.parseBoolean(value);
                        case NOTB -> ~Integer.parseInt(value);
                        default -> null;
                    });
            return true;
        }
        return false;
    }

    private boolean dealWithBinaryOp(String destVar,BinaryOpInstruction binaryOp, Map<String, String> varTable) {
        String leftValue = "";
        if (!binaryOp.getLeftOperand().isLiteral()) {
            leftValue = ((LiteralElement) binaryOp.getLeftOperand()).getLiteral();
        } else {
            Operand leftOp = ((Operand) binaryOp.getLeftOperand());
            if (varTable.containsKey(leftOp.getName())) {
                leftValue = varTable.get(leftOp.getName());
            }
        }
        String rightValue = "";
        if (!binaryOp.getRightOperand().isLiteral()) {
            rightValue = ((LiteralElement) binaryOp.getRightOperand()).getLiteral();
        } else {
            Operand rightOp = ((Operand) binaryOp.getRightOperand());
            if (varTable.containsKey(rightOp.getName())) {
                rightValue = varTable.get(rightOp.getName());
            }
        }
        if (!leftValue.isEmpty() && !rightValue.isEmpty()) {
            varTable.put(destVar, "" +
                    switch (binaryOp.getOperation().getOpType()) {
                        case ADD -> Integer.parseInt(leftValue) + Integer.parseInt(rightValue);
                        case SUB -> Integer.parseInt(leftValue) - Integer.parseInt(rightValue);
                        case MUL -> Integer.parseInt(leftValue) * Integer.parseInt(rightValue);
                        case DIV -> Integer.parseInt(leftValue) / Integer.parseInt(rightValue);
                        case SHR -> Integer.parseInt(leftValue) >> Integer.parseInt(rightValue);
                        case SHL -> Integer.parseInt(leftValue) << Integer.parseInt(rightValue);
                        case SHRR -> Integer.parseInt(leftValue) >>> Integer.parseInt(rightValue);
                        case XOR -> Boolean.parseBoolean(leftValue) ^ Boolean.parseBoolean(rightValue);
                        case AND -> Boolean.parseBoolean(leftValue) && Boolean.parseBoolean(rightValue);
                        case OR -> Boolean.parseBoolean(leftValue) || Boolean.parseBoolean(rightValue);
                        case LTH -> Integer.parseInt(leftValue) < Integer.parseInt(rightValue);
                        case GTH -> Integer.parseInt(leftValue) > Integer.parseInt(rightValue);
                        case EQ -> leftValue.equals(rightValue);
                        case NEQ -> !leftValue.equals(rightValue);
                        case LTE -> Integer.parseInt(leftValue) <= Integer.parseInt(rightValue);
                        case GTE -> Integer.parseInt(leftValue) >= Integer.parseInt(rightValue);
                        default -> null;
                    });
            return true;
        }

        return false;
    }


}
