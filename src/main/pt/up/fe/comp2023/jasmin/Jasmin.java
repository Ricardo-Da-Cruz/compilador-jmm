package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Field;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast2jasmin.AstToJasmin;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.ollir.Ollir;

import java.util.Collections;

import static pt.up.fe.comp2023.jasmin.MethodSpecsGetter.translateType;

public class Jasmin implements JasminBackend, AstToJasmin {

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        StringBuilder bytecode = new StringBuilder();
        ClassUnit oC = ollirResult.getOllirClass(); // the Ollir class

        bytecode.append(getPreClass(oC));
        bytecode.append("\n");
        bytecode.append(getSuper(oC));
        bytecode.append("\n");
        bytecode.append("\n");
        bytecode.append(getFields(oC));
        bytecode.append("\n");
        bytecode.append(getMethods(oC));

        System.out.println(bytecode);

        return new JasminResult(ollirResult, bytecode.toString(), Collections.emptyList());
    }

    private String getPreClass(ClassUnit oC) {
        StringBuilder preClassAttachments = new StringBuilder(".class ");

        if (oC.isFinalClass()) {
            preClassAttachments.append("final ");
        }

        if (oC.isStaticClass()) {
            preClassAttachments.append("static ");
        }

        if (oC.getClassAccessModifier().toString().equals("DEFAULT")) {
            preClassAttachments.append("public ");
        } else {
            preClassAttachments.append(oC.getClassAccessModifier().toString()).append(" ");
        }

        if (oC.getPackage() != null) {
            preClassAttachments.append(oC.getPackage()).append("/");
        }

        preClassAttachments.append(oC.getClassName());

        return preClassAttachments.toString();
    }

    private String getSuper(ClassUnit oC) {
        if (oC.getSuperClass() == null) {
            oC.setSuperClass("java/lang/Object");
        }
        return ".super " + oC.getSuperClass();
    }

    private String getFields(ClassUnit oC) {
        StringBuilder fieldsString = new StringBuilder();

        for (Field field: oC.getFields()) {
            fieldsString.append(".field ");

            if (field.getFieldAccessModifier().toString().equals("DEFAULT")) {
                fieldsString.append("private ");
            } else {
                fieldsString.append(field.getFieldAccessModifier().toString().toLowerCase()).append(" ");
            }

            if (field.isFinalField()) {
                fieldsString.append("final ");
            }

            if (field.isStaticField()) {
                fieldsString.append("static ");
            }

            fieldsString.append(field.getFieldName()).append(" ");
            fieldsString.append(translateType(oC, field.getFieldType()));

            fieldsString.append("\n");
        }

        return fieldsString.toString();
    }

    private String getMethods(ClassUnit oC) {
        StringBuilder methodsString = new StringBuilder();
        MethodSpecsGetter specsGetter = new MethodSpecsGetter();

        for(Method method: oC.getMethods()) {
            specsGetter.setMethod(method);

            methodsString.append(specsGetter.getMethodDefinition());

            methodsString.append("\n");
        }

        return methodsString.toString();
    }

    @Override
    public JasminResult toJasmin(JmmSemanticsResult jmmSemanticsResult) {


        //OlliR stage
        Ollir ollir = new Ollir();
        var result = ollir.toOllir(jmmSemanticsResult);

        // Jasmin stage
        Jasmin jasmin = new Jasmin();
        return jasmin.toJasmin(result);
    }
}
