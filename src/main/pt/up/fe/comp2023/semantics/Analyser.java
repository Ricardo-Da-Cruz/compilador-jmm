package pt.up.fe.comp2023.semantics;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.ClassTable;

public class Analyser implements JmmAnalysis {


    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        var rootNode = jmmParserResult.getRootNode();

        ClassTable classTable = new ClassTable(rootNode);

        SemanticVisitor semanticVisitor = new SemanticVisitor(classTable);
        semanticVisitor.buildVisitor();
        semanticVisitor.visit(rootNode, new Pair<>("", ""));

        System.out.println("Semantic Errors:");
        for (var error : semanticVisitor.getErrors())
            System.out.println(error);

        return new JmmSemanticsResult(jmmParserResult, classTable, semanticVisitor.getErrors());


    }
}
