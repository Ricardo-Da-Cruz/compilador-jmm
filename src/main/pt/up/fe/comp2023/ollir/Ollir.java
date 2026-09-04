package pt.up.fe.comp2023.ollir;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.optimization.ASTConstantPropagation;


public class Ollir implements JmmOptimization {


    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {

        OllirGenerator generator = new OllirGenerator(jmmSemanticsResult.getSymbolTable());
        String code = generator.visit(jmmSemanticsResult.getRootNode(), new Pair<>("","")).toString();
        System.out.println(code);
        return new OllirResult(code,null);

    }
    /*
    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        OllirOptimizer optimizer = new OllirOptimizer(ollirResult.getOllirClass());
        ClassUnit classUnit = optimizer.optimize();
        System.out.println("jdfnb");
        ollirResult.getOllirClass();
        return null;
    }

     */

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult jmmSemanticsResult) {
        ASTConstantPropagation astConstantPropagation = new ASTConstantPropagation(jmmSemanticsResult.getSymbolTable());
        astConstantPropagation.visit(jmmSemanticsResult.getRootNode(),0);
        return jmmSemanticsResult;
    }



}
